package com.seppius.i18n.plurals;
/**
 * Plural rules for Polish language:
 * <p/>
 * Locales: pl
 * <p/>
 * Languages:
 * - Polish (pl)
 * <p/>
 * Rules:
 * one → n is 1;
 * few → n mod 10 in 2..4 and n mod 100 not in 12..14 and n mod 100 not in 22..24;
 * other → everything else (fractions)
 * <p/>
 * Reference CLDR Version 1.9 beta (2010-11-16 21:48:45 GMT)
 *
 * @package I18n_Plural
 * @category Plural Rules
 * @author Korney Czukowski
 * @copyright (c) 2011 Korney Czukowski
 * @license MIT License
 * @see http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html
 * @see http://unicode.org/repos/cldr/trunk/common/supplemental/plurals.xml
 * @see plurals.xml (local copy)
 */

/**
 * Converted to Java by Sam Marshak, 2012
 */
public class PluralRules_Polish extends PluralRules {
    public int quantityForNumber(int count) {
        int rem100 = count % 100;
        int rem10 = count % 10;

        if (count == 1) {
            return QUANTITY_ONE;
        } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14) && !(rem100 >= 22 && rem100 <= 24)) {
            return QUANTITY_FEW;
        } else {
            return QUANTITY_OTHER;
        }
    }
}