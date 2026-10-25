#!/usr/bin/env python3
"""Assemble an APK (aapt2 output + classes.dex), 4-byte align stored entries,
and sign it with APK Signature Scheme v2 (RSA-2048 / SHA-256).

usage: pack_sign.py <aapt2.apk> <classes.dex> <keystore.pem> <out.apk>
"""
import hashlib, os, struct, sys, zipfile, datetime
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding

STORE_EXT = ('.arsc', '.png', '.jpg', '.so')


def build_zip(base, dex, out):
    with zipfile.ZipFile(base) as zin, open(out, 'wb') as f:
        zout = zipfile.ZipFile(f, 'w')
        entries = [(i, zin.read(i.filename)) for i in zin.infolist()]
        entries.append((zipfile.ZipInfo('classes.dex', (2026, 1, 1, 0, 0, 0)), open(dex, 'rb').read()))
        for info, data in entries:
            zi = zipfile.ZipInfo(info.filename, info.date_time)
            stored = info.filename.endswith(STORE_EXT)
            zi.compress_type = zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED
            zi.external_attr = 0o644 << 16
            if stored:
                # local header = 30 + name + extra; pad extra so data starts 4-aligned
                hdr = f.tell() + 30 + len(zi.filename.encode())
                pad = (-hdr) % 4
                zi.extra = b'\x00' * pad
            zout.writestr(zi, data)
        zout.close()


def lp(b):
    return struct.pack('<I', len(b)) + b


def chunk_digest(data):
    out = []
    for i in range(0, len(data), 1 << 20):
        c = data[i:i + (1 << 20)]
        out.append(hashlib.sha256(b'\xa5' + struct.pack('<I', len(c)) + c).digest())
    return out


def load_key(path):
    if os.path.exists(path):
        pem = open(path, 'rb').read()
        key = serialization.load_pem_private_key(pem, None)
        cert = x509.load_pem_x509_certificate(pem)
        return key, cert
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Free Will Prototype')])
    now = datetime.datetime(2026, 1, 1)
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name)
            .public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(now).not_valid_after(now + datetime.timedelta(days=365 * 30))
            .sign(key, hashes.SHA256()))
    with open(path, 'wb') as f:
        f.write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                  serialization.NoEncryption()))
        f.write(cert.public_bytes(serialization.Encoding.PEM))
    return key, cert


def sign_v2(apk, key, cert, out):
    data = open(apk, 'rb').read()
    eocd = data.rfind(b'PK\x05\x06')
    cd_size, cd_off = struct.unpack('<II', data[eocd + 12:eocd + 20])
    contents, cd, eocd_b = data[:cd_off], data[cd_off:cd_off + cd_size], data[eocd:]

    chunks = chunk_digest(contents) + chunk_digest(cd) + chunk_digest(eocd_b)
    top = hashlib.sha256(b'\x5a' + struct.pack('<I', len(chunks)) + b''.join(chunks)).digest()

    ALG = 0x0103  # RSASSA-PKCS1-v1_5 with SHA2-256
    cert_der = cert.public_bytes(serialization.Encoding.DER)
    signed_data = (lp(lp(struct.pack('<I', ALG) + lp(top)))   # digests
                   + lp(lp(cert_der))                           # certificates
                   + lp(b''))                                   # additional attributes
    sig = key.sign(signed_data, padding.PKCS1v15(), hashes.SHA256())
    pub = key.public_key().public_bytes(serialization.Encoding.DER,
                                        serialization.PublicFormat.SubjectPublicKeyInfo)
    signer = lp(signed_data) + lp(lp(struct.pack('<I', ALG) + lp(sig))) + lp(pub)
    value = lp(lp(signer))

    pair = struct.pack('<Q', 4 + len(value)) + struct.pack('<I', 0x7109871a) + value
    size = len(pair) + 8 + 16
    block = struct.pack('<Q', size) + pair + struct.pack('<Q', size) + b'APK Sig Block 42'

    new_eocd = eocd_b[:16] + struct.pack('<I', cd_off + len(block)) + eocd_b[20:]
    with open(out, 'wb') as f:
        f.write(contents + block + cd + new_eocd)


if __name__ == '__main__':
    base, dex, keyfile, out = sys.argv[1:5]
    tmp = out + '.unsigned'
    build_zip(base, dex, tmp)
    key, cert = load_key(keyfile)
    sign_v2(tmp, key, cert, out)
    os.remove(tmp)
    print('signed', out, os.path.getsize(out), 'bytes')
