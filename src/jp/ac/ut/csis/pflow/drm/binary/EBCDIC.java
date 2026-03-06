/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.drm.binary;

public enum EBCDIC {
    V_0(240, '0'),
    V_1(241, '1'),
    V_2(242, '2'),
    V_3(243, '3'),
    V_4(244, '4'),
    V_5(245, '5'),
    V_6(246, '6'),
    V_7(247, '7'),
    V_8(248, '8'),
    V_9(249, '9'),
    V_A(193, 'A'),
    V_B(194, 'B'),
    V_C(195, 'C'),
    V_D(196, 'D'),
    V_E(197, 'E'),
    V_F(198, 'F'),
    V_G(199, 'G'),
    V_H(200, 'H'),
    V_I(201, 'I'),
    V_J(209, 'J'),
    V_K(210, 'K'),
    V_L(211, 'L'),
    V_M(212, 'M'),
    V_N(213, 'N'),
    V_O(214, 'O'),
    V_P(215, 'P'),
    V_Q(216, 'Q'),
    V_R(217, 'R'),
    V_S(226, 'S'),
    V_T(227, 'T'),
    V_U(228, 'U'),
    V_V(229, 'V'),
    V_W(230, 'W'),
    V_X(231, 'X'),
    V_Y(232, 'Y'),
    V_Z(233, 'Z'),
    V_SPACE(64, ' '),
    V_AMP(80, '&'),
    V_HYPHEN(96, '-'),
    V_SLASH(97, '/'),
    V_COLON(122, ':'),
    V_DOT(75, '.'),
    V_COMMA(107, ','),
    V_SHARP(123, '#'),
    V_LT(76, '<'),
    V_ASTERISK(92, '*'),
    V_PERCENT(108, '%'),
    V_ATMARK(124, '@'),
    V_OPEN_BRACKET(77, '('),
    V_CLOSE_BRACKET(93, ')'),
    V_UNDER_BAR(109, '_'),
    V_SINGLE_QUART(125, '\''),
    V_PLUS(78, '+'),
    V_SEMI_COLON(94, ';'),
    V_GT(110, '>'),
    V_EQUAL(126, '='),
    V_QUESTION(111, '?'),
    V_DOUBLE_QUART(127, '\"');

    private byte _code_value;
    private char _character;

    public static char convert(byte ebcdic_value) {
        EBCDIC[] eBCDICArray = EBCDIC.values();
        int n = eBCDICArray.length;
        int n2 = 0;
        while (n2 < n) {
            EBCDIC e = eBCDICArray[n2];
            if (e._code_value == ebcdic_value) {
                return e._character;
            }
            ++n2;
        }
        return '\u0000';
    }

    private EBCDIC(int code_value, char character) {
        this._code_value = (byte)code_value;
        this._character = character;
    }

    public byte getCodeValue() {
        return this._code_value;
    }

    public char getCharacter() {
        return this._character;
    }

    public String toString() {
        return String.valueOf(String.valueOf(this._code_value)) + ":" + String.valueOf(this._character);
    }
}

