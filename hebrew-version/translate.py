#!/usr/bin/env python3
"""Builds the Hebrew tester sources from the main app.

Copies ../src, ../res and ../assets into this folder, then rewrites every user-facing
string in Hebrew, switches the serif face to Frank Ruhl Libre, lays the screens out
right-to-left and gives the keyboard a Hebrew layout (with an ABC toggle).

Run it whenever the main app changes, then ./build.sh. Every replacement is checked:
if an English string it expects has changed, the script stops and names it.
"""
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PKG = "src/com/freewill/predict"


def copy_tree():
    for d in ("src", "res", "assets"):
        dst = os.path.join(HERE, d)
        if os.path.exists(dst):
            shutil.rmtree(dst)
        shutil.copytree(os.path.join(ROOT, d), dst)
    shutil.copy(os.path.join(HERE, "fonts", "frl-medium.ttf"), os.path.join(HERE, "assets/fonts"))
    shutil.copy(os.path.join(HERE, "fonts", "frl-bold.ttf"), os.path.join(HERE, "assets/fonts"))


# ------------------------------------------------------------------ translations

TAGS = [
    ('"{spectator}"', '"{צופה}"'),
    ('"{magician}"', '"{קוסם}"'),
    ('"{spectator\'s object}"', '"{חפץ הצופה}"'),
    ('"{magician\'s object}"', '"{חפץ הקוסם}"'),
    ('"{remaining object}"', '"{חפץ נותר}"'),
    ('"{item}"', '"{חפץ}"'),
    ('"{pointed item}"', '"{חפץ מוצבע}"'),
    ('"{number}"', '"{מספר}"'),
    ('"{symbol}"', '"{סמל}"'),
    ('"{outcome}"', '"{תוצאה}"'),
    ('return "{flip " + k + "}";', 'return "{הטלה " + k + "}";'),
]

HEB_DEFAULTS = '''    static final String DEF_OBJECTS = "ל" + T_SPECTATOR + " יש את ה" + T_S_OBJ + ", ל"
            + T_MAGICIAN + " יש את ה" + T_M_OBJ + ", וה" + T_REST + " בכוס.";
    static final String DEF_EIGHT_PROPHECY = "מכל מה שעל השולחן, הבחירה של " + T_SPECTATOR + " תהיה ה" + T_ITEM + ".";
    static final String DEF_EIGHT_FORCE = "למרות ההצבעה של " + T_MAGICIAN + " על ה" + T_POINTED + ", "
            + "הבחירה של " + T_SPECTATOR + " נמשכה בדיוק אל ה" + T_ITEM + ".";
    static final String DEF_DIE = "הקובייה של " + T_SPECTATOR + " תיפול על " + T_NUMBER + ".";
    static final String DEF_ESP = "הסמל של " + T_SPECTATOR + " הוא ה" + T_SYMBOL + ".";
    static final String DEF_COIN_OPEN = "זה היה ידוע ל" + T_MAGICIAN + " עוד לפני ההטלות של " + T_SPECTATOR + ":";
    static final String[] DEF_COIN_LINES = {
            "ההטלה הראשונה תהיה " + flipTag(1) + ".",
            "השנייה תהיה " + flipTag(2) + ".",
            "והאחרונה תהיה " + flipTag(3) + "."};

    public static final String[] EIGHT_FLAVOR = {
            "המבט של " + T_MAGICIAN + " היה על ה" + T_POINTED + ", אבל היד של " + T_SPECTATOR + " הלכה אל ה" + T_ITEM + ".",
            "בכל פעם שהיד של " + T_MAGICIAN + " נגעה ב" + T_POINTED + ", הבחירה של " + T_SPECTATOR + " התקרבה אל ה" + T_ITEM + ".",
    };
'''

HEB_IDEAS = '''    public static final Idea[] IDEAS = {
            new Idea("התאמת צבעים", "עד 8 צבעים. הקהל מצביע בהרמת ידיים; התחזית מציינת את הצבע המנצח.",
                    "הצבע האהוב הערב יהיה " + T_OUTCOME + ".",
                    "אדום", "כחול", "ירוק", "צהוב", "סגול", "כתום"),
            new Idea("אבן, נייר ומספריים", "3 תוצאות. אתם תמיד זורקים אבן; התחזית אומרת מה הצופה זורק ומי מנצח.",
                    "הבחירה של " + T_SPECTATOR + ": " + T_OUTCOME,
                    "אבן, ותיקו.", "נייר, והניצחון של " + T_SPECTATOR + ".", "מספריים, והניצחון של " + T_MAGICIAN + "."),
            new Idea("תעלומת רצח", "8 תוצאות. 2 חשודים × 2 כלי רצח × 2 חדרים, מסופרים כסיפור אחד.",
                    "זה היה " + T_OUTCOME + ".",
                    "המשרת, עם החבל, בספרייה", "המשרת, עם החבל, במטבח",
                    "המשרת, עם הפמוט, בספרייה", "המשרת, עם הפמוט, במטבח",
                    "הטבחית, עם החבל, בספרייה", "הטבחית, עם החבל, במטבח",
                    "הטבחית, עם הפמוט, בספרייה", "הטבחית, עם הפמוט, במטבח"),
            new Idea("סידור ישיבה", "6 תוצאות. שלושה אנשים, שלושה כיסאות, כל סידור אפשרי.",
                    "משמאל לימין: " + T_OUTCOME + ".",
                    "אדם 1, אדם 2, אדם 3", "אדם 1, אדם 3, אדם 2", "אדם 2, אדם 1, אדם 3",
                    "אדם 2, אדם 3, אדם 1", "אדם 3, אדם 1, אדם 2", "אדם 3, אדם 2, אדם 1"),
    };
'''

ROUTINES = TAGS + [
    ('"Front", "Front · pause", "Back", "Back · pause",',
     '"קדמי", "קדמי · עצירה", "אחורי", "אחורי · עצירה",'),
    ('"Left", "Left · pause", "Right", "Right · pause"};',
     '"שמאל", "שמאל · עצירה", "ימין", "ימין · עצירה"};'),
    ('"The Normal Reveal"', '"החשיפה הרגילה"'),
    ('"The Expert Reveal"', '"חשיפת המומחה"'),
    ('"The Eight"', '"השמונה"'),
    ('"Die Roll"', '"הטלת קובייה"'),
    ('"ESP Symbols"', '"סמלי ESP"'),
    ('"Coin Flips"', '"הטלות מטבע"'),
    ('"New Routine"', '"רוטינה חדשה"'),
    ('return "Normal";', 'return "רגילה";'),
    ('return "Expert";', 'return "מומחה";'),
    ('return "Eight";', 'return "שמונה";'),
    ('return "Die";', 'return "קובייה";'),
    ('return "Coins";', 'return "מטבעות";'),
    ('"Custom"', '"אישית"'),
    ('"2 outcomes · one object forced"', '"2 תוצאות · חפץ אחד מאולץ"'),
    ('"6 outcomes · no force"', '"6 תוצאות · בלי פורס"'),
    ('"8 items · mental force"', '"8 חפצים · פורס מנטלי"'),
    ('"8 items · prophecy"', '"8 חפצים · נבואה"'),
    ('"6 outcomes · a real die"', '"6 תוצאות · קובייה אמיתית"'),
    ('"5 symbols · counted by lines"', '"5 סמלים · לפי מספר הקווים"'),
    ('return n + (n == 1 ? " flip · " : " flips · ") + (1 << n) + " outcomes";',
     'return (n == 1 ? "הטלה אחת · " : n + " הטלות · ") + (1 << n) + " תוצאות";'),
    ('" outcomes · built by you"', '" תוצאות · בנויה על ידיכם"'),
    ('"Three objects, one of them forced. Whoever picks the phone up decides who holds which of the other two."',
     '"שלושה חפצים, אחד מהם מאולץ. מי שמרים את הטלפון קובע מי מחזיק באיזה משני האחרים."'),
    ('"Three objects, three places, no force. However the choices fall, the way you lift the phone writes the one prediction that matches."',
     '"שלושה חפצים, שלושה מקומות, בלי פורס. איך שהבחירות לא ייפלו, הדרך שבה מרימים את הטלפון כותבת את התחזית היחידה שמתאימה."'),
    ('"Eight items on the table. The spectator freely chooses one, and every edge of the phone, with or without a pause, names a different item."',
     '"שמונה חפצים על השולחן ובחירה חופשית של אחד מהם. כל צד של הטלפון, עם עצירה או בלי, מציין חפץ אחר."'),
    ('"The spectator rolls a real die. Whatever lands face up was already written on your phone."',
     '"הצופה מטיל קובייה אמיתית. מה שיוצא למעלה כבר כתוב בטלפון שלכם."'),
    ('"The five classic Zener symbols. The spectator chooses or thinks of one, and your phone already holds the answer."',
     '"חמשת סמלי זנר הקלאסיים. הצופה בוחר או חושב על אחד, והטלפון שלכם כבר מחזיק את התשובה."'),
    ('"The spectator flips a coin one, two or three times. Your phone predicted every flip, in order."',
     '"הצופה מטיל מטבע פעם, פעמיים או שלוש. הטלפון שלכם חזה כל הטלה, לפי הסדר."'),
    ('"Your own routine: two to eight outcomes, each on the pickup you choose."',
     '"רוטינה משלכם: שתיים עד שמונה תוצאות, כל אחת בהרמה שתבחרו."'),
    ('"the name you type"', '"השם שמקלידים"'),
    ('"your name, above"', '"השם שלכם, למעלה"'),
    ('"what the spectator holds"', '"מה שהצופה מחזיק"'),
    ('"what you hold"', '"מה שאתם מחזיקים"'),
    ('"the one left over (always Object 3)"', '"זה שנשאר (תמיד חפץ 3)"'),
    ('"the one left over"', '"זה שנשאר"'),
    ('"what you point at, set above"', '"מה שאתם מצביעים עליו, נקבע למעלה"'),
    ('"the item the pickup points to"', '"החפץ שההרמה מצביעה עליו"'),
    ('"the number rolled, 1 to 6"', '"המספר שיצא, 1 עד 6"'),
    ('"the chosen symbol\'s name"', '"שם הסמל שנבחר"'),
    ('"flip " + k + ": your word for heads or tails"', '"הטלה " + k + ": המילה שלכם לעץ או לפלי"'),
    ('"the outcome the pickup points to"', '"התוצאה שההרמה מצביעה עליה"'),
    ('{"Ring", "Marker", "Coin"}', '{"טבעת", "טוש", "מטבע"}'),
    ('{"Key", "Watch", "Ring", "Coin", "Pen", "Lighter", "Card", "Glasses"}',
     '{"מפתח", "שעון", "טבעת", "מטבע", "עט", "מצית", "קלף", "משקפיים"}'),
    ('{"one", "two", "three", "four", "five", "six"}', '{"אחת", "שתיים", "שלוש", "ארבע", "חמש", "שש"}'),
    ('{"Circle", "Cross", "Waves", "Square", "Star"}', '{"עיגול", "צלב", "גלים", "ריבוע", "כוכב"}'),
    ('"heads", "Heads")', '"heads", "עץ")'),
    ('"tails", "Tails")', '"tails", "פלי")'),
]

BASE = [
    ('"fonts/cinzel-medium.ttf");\n            serifBold', '"fonts/frl-medium.ttf");\n            serifBold'),
    ('serifBold = Typeface.createFromAsset(getAssets(), "fonts/cinzel-bold.ttf");',
     'serifBold = Typeface.createFromAsset(getAssets(), "fonts/frl-bold.ttf");'),
    ('        prefs = Prefs.get(this);\n',
     '        prefs = Prefs.get(this);\n'
     '        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);\n'),
    # Hebrew letters don't want the wide tracking the Latin capitals use; the logo keeps it.
    ('    void centered(TextView t, float spacingEm) {\n',
     '    void centered(TextView t, float spacingEm) {\n'
     '        if (t.getTypeface() != logo) spacingEm = 0.02f;\n'),
    ('"%.1f s"', '"%.1f שנ׳"'),
    ('"THE VANISH"', '"ההיעלמות"'),
    ('"After the prediction appears"', '"אחרי שהתחזית מופיעה"'),
    ('"Switch back to my keyboard and hide it"', '"חזרה למקלדת שלי והסתרה"'),
    ('"Switch back to my keyboard"', '"חזרה למקלדת שלי"'),
    ('"Hide the keyboard"', '"הסתרת המקלדת"'),
    ('"Do nothing"', '"לא לעשות כלום"'),
    ('"Silent tap when armed"', '"רטט שקט כשהטלפון דרוך"'),
    ('"Trigger tilt"', '"זווית הפעלה"'),
    ('"Lower fires earlier in the pickup, with the screen still angled away."',
     '"זווית נמוכה יותר מפעילה מוקדם יותר בהרמה, כשהמסך עדיין מוטה הרחק מהצופים."'),
    ('"Pause length"', '"משך העצירה"'),
    ('"SPECTATOR SITS HERE"', '"כאן יושב הצופה"'),
    ('"YOU SIT HERE"', '"כאן אתם יושבים"'),
    ('"‖ = with a pause"', '"‖ = עם עצירה"'),
    ('fillCell(front, "Front",', 'fillCell(front, "קדמי",'),
    ('fillCell(back, "Back",', 'fillCell(back, "אחורי",'),
    ('fillCell(left, "Left",', 'fillCell(left, "שמאל",'),
    ('fillCell(right, "Right",', 'fillCell(right, "ימין",'),
    ('"unused"', '"לא בשימוש"'),
    ('"TOP"', '"למעלה"'),
    ('"face down"', '"הפוך"'),
]

HOME = [
    ('"THE ROUTINES"', '"הרוטינות"'),
    ('"Type the spectator\'s name, then the routine\'s number. No number runs routine I."',
     '"הקלידו את שם הצופה ואחריו את מספר הרוטינה. בלי מספר תופעל רוטינה I."'),
    ('"Keyboard enabled"', '"המקלדת פעילה"'),
    ('"Keyboard not enabled"', '"המקלדת לא פעילה"'),
    ('"Listed as “Thrill” in your keyboard picker"', '"מופיעה בשם „Thrill עברית” בבורר המקלדות"'),
    ('"Turn on “Thrill” in your keyboard settings. Android warns about every keyboard; this one sends nothing."',
     '"הפעילו את „Thrill עברית” בהגדרות המקלדת. אנדרואיד מזהיר על כל מקלדת; המקלדת הזו לא שולחת דבר."'),
    ('"Switch"', '"החלפה"'),
    ('"Enable"', '"הפעלה"'),
    ('"Thrill emblem"', '"הסמל של Thrill"'),
    ('"PREDICTION KEYBOARD · " + VERSION.toUpperCase()', '"מקלדת תחזיות · עברית · " + VERSION'),
    ('"Noa · Noa1" : "Noa" + r', '"נועה · נועה1" : "נועה" + r'),
    ('text("›", 26', 'text("‹", 26'),
    ('"BUILD IT YOURSELF"', '"בנו בעצמכם"'),
    ('"2 to 8 outcomes, on the pickups you choose · becomes routine "',
     '"2 עד 8 תוצאות, בהרמות שתבחרו · תהיה רוטינה "'),
    ('"CREATED BY", "Yair Sarig"', '"נוצר על ידי", "יאיר שריג"'),
    ('"PRESENTED BY"', '"מוגש על ידי"'),
]

REHEARSAL = [
    ('"REHEARSAL"', '"אימון"'),
    ('"Choose a routine, then lay the phone down."', '"בחרו רוטינה, ואז הניחו את הטלפון."'),
    ('lockedName + " only. Lay the phone down and pick it up."',
     '"רק " + lockedName + ". הניחו את הטלפון והרימו אותו."'),
    ('"No gyroscope found: who picked it up will be guessed."',
     '"לא נמצא גירוסקופ: מי שהרים את הטלפון ינוחש."'),
    ('"PAUSED"', '"עצירה"'),
    ('"Pause registered. Finish the pickup."', '"העצירה נקלטה. סיימו את ההרמה."'),
    ('"WAITING"', '"ממתין"'),
    ('"Lay it face down, bottom pointing at you."', '"הניחו אותו הפוך, כשהתחתית מכוונת אליכם."'),
    ('"ARMED"', '"דרוך"'),
    ('"Lift an edge, or lift slightly and hold still for a pause."',
     '"הרימו צד, או הרימו מעט והחזיקו בלי לזוז לעצירה."'),
    ('"Face down and waiting. Pick it up to see which reveal fires."',
     '"הפוך וממתין. הרימו כדי לראות איזו חשיפה מופעלת."'),
    ('"RISING"', '"עולה"'),
    ('"REVEALED"', '"נחשף"'),
    ('"tilt %.0f°', '"הטיה %.0f°'),
]

DETECTOR = [
    ('"Picked up by the spectator"', '"הצופה הרים"'),
    ('"Picked up by the magician"', '"הקוסם הרים"'),
    ('"Front edge · pause"', '"צד קדמי · עצירה"'),
    ('"Front edge"', '"צד קדמי"'),
    ('"Back edge · pause"', '"צד אחורי · עצירה"'),
    ('"Back edge"', '"צד אחורי"'),
    ('"Left edge"', '"צד שמאל"'),
    ('"Right edge"', '"צד ימין"'),
]

PREFS = [
    ('"Magician"', '"קוסם"'),
    ('"Spectator"', '"צופה"'),
]

ROUTINE_PAGE = [
    ('"Delete this routine"', '"מחיקת הרוטינה"'),
    ('text("‹", 30', 'text("›", 30'),
    ('"Back to routines"', '"חזרה לרוטינות"'),
    ('"Type the name, or the name + 1"', '"הקלידו את השם, או את השם + 1"'),
    ('"Build It Yourself · type the name + "', '"בנו בעצמכם · הקלידו את השם + "'),
    ('"Type the name + "', '"הקלידו את השם + "'),
    ('field("Magician"', 'field("הקוסם"'),
    ('"Spectator if none typed"', '"הצופה, אם לא הוקלד שם"'),
    ('"THE PLAYERS"', '"המשתתפים"'),
    ('"Object " + (i + 1)', '"חפץ " + (i + 1)'),
    ('" · forced"', '" · מאולץ"'),
    ('"The eight items"', '"שמונת החפצים"'),
    ('"Write numbers as"', '"כתיבת המספרים"'),
    ('{"Digits", "Words"}, new String[]{"4", "four"}', '{"ספרות", "מילים"}, new String[]{"4", "ארבע"}'),
    ('"Symbol names, as they appear in the prediction"', '"שמות הסמלים, כפי שיופיעו בתחזית"'),
    ('"Number of flips"', '"מספר ההטלות"'),
    ('{"2 outs", "4 outs", "8 outs"}', '{"2 אפשרויות", "4 אפשרויות", "8 אפשרויות"}'),
    ('"Word for heads"', '"המילה לעץ"'),
    ('"Word for tails"', '"המילה לפלי"'),
    ('"THE PROPHECY"', '"הנבואה"'),
    ('"Prediction text"', '"טקסט התחזית"'),
    ('"e.g. Tonight\'s favourite colour will be {outcome}."', '"למשל: הצבע האהוב הערב יהיה {תוצאה}."'),
    ('"Leave it empty to write just the outcome."', '"השאירו ריק כדי לכתוב רק את התוצאה."'),
    ('"THE MENTAL FORCE"', '"הפורס המנטלי"'),
    ('{"Prophecy", "Mental Force"}', '{"נבואה", "פורס מנטלי"}'),
    ('"“I knew you\'d choose…”", "“I made you choose…”"', '"„ידעתי מה תבחרו…”", "„גרמתי לכם לבחור…”"'),
    ('"You point at (chosen in advance from the eight items)"',
     '"אתם מצביעים על (נבחר מראש מתוך שמונת החפצים)"'),
    ('"The list follows the item names above."', '"הרשימה מתעדכנת לפי שמות החפצים למעלה."'),
    ('"Force text"', '"טקסט הפורס"'),
    ('"FLAVOR IDEAS · TAP TO USE"', '"רעיונות לניסוח · הקישו לשימוש"'),
    ('subtitle("One line per flip. With " + n + (n == 1 ? " flip" : " flips") + " you write "\n'
     '                + (n == 1 ? "one line" : n + " lines") + ", plus an optional opening.")',
     'subtitle("שורה אחת לכל הטלה. עם " + (n == 1 ? "הטלה אחת" : n + " הטלות") + " כותבים "\n'
     '                + (n == 1 ? "שורה אחת" : n + " שורות") + ", ועוד שורת פתיחה לא חובה.")'),
    ('"Opening line"', '"שורת פתיחה"'),
    ('"Optional"', '"לא חובה"'),
    ('{"Flip 1", "Flip 2", "Flip 3"}', '{"הטלה 1", "הטלה 2", "הטלה 3"}'),
    ('"Tap a tag to insert it into the opening line, or type it into any line."',
     '"הקישו על תגית כדי להוסיף אותה לשורת הפתיחה, או הקלידו אותה בכל שורה."'),
    ('"THE REVEAL"', '"החשיפה"'),
    ('"Whoever picks the phone up gets Object 1, the other person Object 2. Object 3 is forced."',
     '"מי שמרים את הטלפון מקבל את חפץ 1, והשני את חפץ 2. חפץ 3 מאולץ."'),
    ('"SPECTATOR PICKS IT UP"', '"הצופה מרים"'),
    ('"MAGICIAN PICKS IT UP"', '"הקוסם מרים"'),
    ('"Rule: lay it face down, bottom pointing at you."', '"הכלל: מניחים הפוך, כשהתחתית מכוונת אליכם."'),
    ('"Lift the edge that matches. For a paused version, lift slightly, hold still until the silent tap, then finish."',
     '"הרימו את הצד המתאים. לגרסה עם עצירה, הרימו מעט, החזיקו בלי לזוז עד הרטט השקט, ואז סיימו."'),
    ('"Every edge has two versions. Straight lift = the first item. Lift slightly, wait for the silent tap, then continue (or flip it over from the side) = the second."',
     '"לכל צד שתי גרסאות. הרמה ישרה = החפץ הראשון. הרמה קלה, המתנה לרטט השקט ואז המשך (או היפוך מהצד) = השני."'),
    ('"Same six pickups as the Expert Reveal. Glance at the die, then lift the matching edge."',
     '"אותן שש הרמות כמו בחשיפת המומחה. הציצו בקובייה והרימו את הצד המתאים."'),
    ('"Count the lines in the symbol and go round clockwise: 1 front, 2 right, 3 back, 4 left, 5 front again with a pause."',
     '"ספרו את הקווים בסמל והתקדמו עם כיוון השעון: 1 קדמי, 2 ימין, 3 אחורי, 4 שמאל, 5 שוב קדמי עם עצירה."'),
    ('"EXAMPLE · "', '"דוגמה · "'),
    ('"EASY TO REMEMBER"', '"קל לזכור"'),
    ('"Heads = front · Tails = back"', '"עץ = קדמי · פלי = אחורי"'),
    ('"1st flip · Heads = front or back · Tails = a side"', '"הטלה 1 · עץ = קדמי או אחורי · פלי = צד"'),
    ('"2nd flip · Heads = front / left · Tails = back / right"', '"הטלה 2 · עץ = קדמי / שמאל · פלי = אחורי / ימין"'),
    ('"3rd flip · Heads = straight lift · Tails = pause"', '"הטלה 3 · עץ = הרמה ישרה · פלי = עצירה"'),
    ('"THE ROUTINE"', '"הרוטינה"'),
    ('"Routine name"', '"שם הרוטינה"'),
    ('"Name your routine"', '"תנו שם לרוטינה"'),
    ('"Both names are available as tags in your prophecy."', '"שני השמות זמינים כתגיות בנבואה."'),
    ('" of "', '" מתוך "'),
    ('"THE OUTCOMES"', '"התוצאות"'),
    ('"Each outcome fills {outcome} in the prophecy. Pick the pickup that writes it."',
     '"כל תוצאה ממלאת את {תוצאה} בנבואה. בחרו את ההרמה שכותבת אותה."'),
    ('"Outcome " + (i + 1)', '"תוצאה " + (i + 1)'),
    ('"Remove outcome "', '"הסרת תוצאה "'),
    ('"+  ADD OUTCOME"', '"+  הוספת תוצאה"'),
    ('"Front", "Front ‖", "Back", "Back ‖", "Left", "Left ‖", "Right", "Right ‖"}',
     '"קדמי", "קדמי ‖", "אחורי", "אחורי ‖", "שמאל", "שמאל ‖", "ימין", "ימין ‖"}'),
    ('heading("IDEAS"', 'heading("רעיונות"'),
    ('"Optional. Nothing is filled in until you tap an idea; then it sets up the outcomes and prophecy for you to edit."',
     '"לא חובה. שום דבר לא מתמלא עד שמקישים על רעיון; אז הוא מגדיר תוצאות ונבואה שאפשר לערוך."'),
    ('"IN USE"', '"בשימוש"'),
    ('"Use “"', '"להשתמש ב„"'),
    ('"This replaces your outcomes and prophecy."', '"פעולה זו מחליפה את התוצאות והנבואה שלכם."'),
    ('"Replace"', '"החלפה"'),
    ('"Cancel"', '"ביטול"'),
    ('"Delete “"', '"למחוק את „"'),
    ('"Delete"', '"מחיקה"'),
    ('"Routine " + Routines.roman(r)', '"רוטינה " + Routines.roman(r)'),
    ('" will be removed. This can\'t be undone."', '" תימחק. אי אפשר לבטל את זה."'),
    ('" will be removed. What should happen to the numbers after it?"', '" תימחק. מה יקרה למספרים שאחריה?"'),
    ('"Keep numbers"', '"שמירת המספרים"'),
    ('"Nothing else changes. "', '"שום דבר אחר לא משתנה. המספר "'),
    ('" stays empty until you build a new routine."', '" יישאר פנוי עד שתבנו רוטינה חדשה."'),
    ('"Renumber"', '"מספור מחדש"'),
    ('"Close all gaps:"', '"סגירת כל הפערים:"'),
    ('" → "', '" ← "'),
]

KEYBOARD = [
    ('    private static final String[] ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};\n',
     '    private static final String[] ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};\n'
     '    private static final String[] HEB = {"\'קראטוןםפ", "שדגכעיחלךף", "זסבהנמצתץ"};\n'
     '    private boolean hebrew = true;\n'
     '    private LinearLayout engRows, hebRows;\n'
     '    private TextView langKey;\n'),
    ('''        // Row 1-2
        root.addView(letterRow(ROWS[0], 0));
        root.addView(letterRow(ROWS[1], 0.5f));
''', '''        // English letters
        engRows = column();
        engRows.addView(letterRow(ROWS[0], 0));
        engRows.addView(letterRow(ROWS[1], 0.5f));
'''),
    ('''        TextView del = special("⌫", 1.5f, v -> backspace());
        r3.addView(del);
        root.addView(r3);
''', '''        r3.addView(special("⌫", 1.5f, v -> backspace()));
        engRows.addView(r3);
        root.addView(engRows);

        // Hebrew letters (standard Israeli layout)
        hebRows = column();
        LinearLayout h1 = row();
        h1.addView(spacer(1f));
        for (char ch : HEB[0].toCharArray()) h1.addView(hebKey(ch));
        hebRows.addView(h1);
        LinearLayout h2 = row();
        for (char ch : HEB[1].toCharArray()) h2.addView(hebKey(ch));
        hebRows.addView(h2);
        LinearLayout h3 = row();
        for (char ch : HEB[2].toCharArray()) h3.addView(hebKey(ch));
        h3.addView(special("⌫", 1.5f, v -> backspace()));
        hebRows.addView(h3);
        root.addView(hebRows);
'''),
    ('        r4.addView(special("🌐", 1.3f, v -> switchKeyboard()));\n',
     '        r4.addView(special("🌐", 1.3f, v -> switchKeyboard()));\n'
     '        langKey = special("", 1.3f, v -> { hebrew = !hebrew; refreshLanguage(); });\n'
     '        r4.addView(langKey);\n'),
    ('        refreshCase();\n        updateDot(false);\n',
     '        refreshCase();\n        refreshLanguage();\n        updateDot(false);\n'),
    ('    private LinearLayout letterRow(String letters, float sidePad) {\n', '''    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    private TextView hebKey(char ch) {
        String s = String.valueOf(ch);
        return key(s, 1f, keyBg, v -> type(s));
    }

    private void refreshLanguage() {
        if (engRows == null) return;
        engRows.setVisibility(hebrew ? View.GONE : View.VISIBLE);
        hebRows.setVisibility(hebrew ? View.VISIBLE : View.GONE);
        langKey.setText(hebrew ? "ABC" : "אב");
    }

    private LinearLayout letterRow(String letters, float sidePad) {
'''),
]

PATCHES = {
    "Routines.java": ROUTINES,
    "BaseActivity.java": BASE,
    "HomeActivity.java": HOME,
    "RehearsalCard.java": REHEARSAL,
    "PickupDetector.java": DETECTOR,
    "Prefs.java": PREFS,
    "RoutineActivity.java": ROUTINE_PAGE,
    "PredictionKeyboard.java": KEYBOARD,
    "SymbolView.java": [('{"Circle", "Cross", "Waves", "Square", "Star"}', '{"עיגול", "צלב", "גלים", "ריבוע", "כוכב"}')],
}


def block(src, start, end_marker, new):
    i = src.index(start)
    j = src.index(end_marker, i) + len(end_marker)
    return src[:i] + new + src[j:]


def patch_sources():
    failed = []
    for name, pairs in PATCHES.items():
        path = os.path.join(HERE, PKG, name)
        with open(path, encoding="utf-8") as f:
            s = f.read()
        if name == "Routines.java":
            s = block(s, "    static final String DEF_OBJECTS", "    };\n", HEB_DEFAULTS)
            s = block(s, "    public static final Idea[] IDEAS = {", "    };\n", HEB_IDEAS)
        for old, new in pairs:
            if old not in s:
                failed.append(f"{name}: {old[:70]!r}")
                continue
            s = s.replace(old, new)
        if name not in ("PredictionKeyboard.java",):
            # Tracking suits Latin capitals, not Hebrew; keep a trace of it.
            s = re.sub(r"setLetterSpacing\(0\.\d+f\)", "setLetterSpacing(0.02f)", s)
        with open(path, "w", encoding="utf-8") as f:
            f.write(s)
    if failed:
        sys.exit("English text changed; update translate.py:\n  " + "\n  ".join(failed))


def write_manifest():
    with open(os.path.join(ROOT, "AndroidManifest.xml"), encoding="utf-8") as f:
        m = f.read()
    assert 'package="com.dailymagician.thrill"' in m
    m = m.replace('package="com.dailymagician.thrill"', 'package="com.dailymagician.thrill.he"')
    m = m.replace('android:label="Thrill"', 'android:label="Thrill עברית"')
    m = m.replace('android:icon="@mipmap/ic_launcher"',
                  'android:icon="@mipmap/ic_launcher"\n        android:supportsRtl="true"')
    for cls in ("HomeActivity", "RoutineActivity", "PredictionKeyboard"):
        m = m.replace(f'android:name=".{cls}"', f'android:name="com.freewill.predict.{cls}"')
    assert m.count("Thrill עברית") == 2 and "supportsRtl" in m and 'name=".' not in m
    with open(os.path.join(HERE, "AndroidManifest.xml"), "w", encoding="utf-8") as f:
        f.write(m)

    path = os.path.join(HERE, "res/xml/method.xml")
    with open(path, encoding="utf-8") as f:
        x = f.read()
    x = x.replace('android:label="English"', 'android:label="עברית"')
    x = x.replace('android:imeSubtypeLocale="en_US"', 'android:imeSubtypeLocale="iw_IL"')
    assert "עברית" in x and "iw_IL" in x
    with open(path, "w", encoding="utf-8") as f:
        f.write(x)


def report_leftovers():
    """Lists string literals that still contain English words, for a human to check."""
    skip = re.compile(r'^(fonts/|r\d|[a-z_]+\d*$|%|freewill|sans-serif|\{first\}|\{second\}|\{obj\d\}|ESP|ABC|THRILL|Thrill|The Daily Magician|[MCDLXVI]+$)')
    for name in sorted(os.listdir(os.path.join(HERE, PKG))):
        with open(os.path.join(HERE, PKG, name), encoding="utf-8") as f:
            for n, line in enumerate(f, 1):
                if line.strip().startswith(("*", "/", "//")):
                    continue
                for lit in re.findall(r'"((?:[^"\\]|\\.)*)"', line):
                    if re.search(r"[A-Za-z]{3,}", lit) and not skip.match(lit) and not re.search(r"[֐-׿]", lit):
                        print(f"  {name}:{n}: {lit}")


if __name__ == "__main__":
    copy_tree()
    patch_sources()
    write_manifest()
    print("Hebrew sources written. English literals left (keys and the like are expected):")
    report_leftovers()
