package com.seppius.i18n.plurals;
/**
 * Plural rules for the following locales and languages
 * <p/>
 * Locales: cs sk
 * <p/>
 * Languages:
 * - Czech (cs)
 * - Slovak (sk)
 * <p/>
 * Rules:
 * one → n is 1;
 * few → n in 2..4;
 * other → everything else
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
public class PluralRules_Czech extends PluralRules {
    public int quantityForNumber(int count) {
        if (count == 1) {
            return QUANTITY_ONE;
        } else if (count >= 2 && count <= 4) {
            return QUANTITY_FEW;
        } else {
            return QUANTITY_OTHER;
        }
    }
}