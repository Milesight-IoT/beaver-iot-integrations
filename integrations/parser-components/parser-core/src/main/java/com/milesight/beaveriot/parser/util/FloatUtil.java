package com.milesight.beaveriot.parser.util;


/**
 * Float工具类
 */
public class FloatUtil {

    /**
     * The number of logical bits in the significand of a
     * <code>float</code> number, including the implicit bit.
     */
    public static final int SIGNIFICAND_WIDTH = 24;

    /**
     * Bias used in representing a <code>float</code> exponent.
     */
    public static final int EXP_BIAS = 127;

    /**
     * 二进制16位浮点数转换成单精度浮点数
     */
    public static float float16ToFloat(short floatBinary16) {
        /*
         * The binary16 format has 1 sign bit, 5 exponent bits, and 10
         * significand bits. The exponent bias is 15.
         */
        int bin16arg = (int) floatBinary16;
        int bin16SignBit = 0x8000 & bin16arg;
        int bin16ExpBits = 0x7c00 & bin16arg;
        int bin16SignifBits = 0x03FF & bin16arg;

        // Shift left difference in the number of significand bits in
        // the float and binary16 formats
        final int SIGNIF_SHIFT = (SIGNIFICAND_WIDTH - 11);

        float sign = (bin16SignBit != 0) ? -1.0f : 1.0f;

        // Extract binary16 exponent, remove its bias, add in the bias
        // of a float exponent and shift to correct bit location
        // (significand width includes the implicit bit so shift one
        // less).
        int bin16Exp = (bin16ExpBits >> 10) - 15;
        if (bin16Exp == -15) {
            // For subnormal binary16 values and 0, the numerical
            // value is 2^24 * the significand as an integer (no
            // implicit bit).
            return sign * (0x1p-24f * bin16SignifBits);
        } else if (bin16Exp == 16) {
            return (bin16SignifBits == 0) ?
                    sign * Float.POSITIVE_INFINITY :
                    Float.intBitsToFloat((bin16SignBit << 16) |
                            0x7f80_0000 |
                            // Preserve NaN signif bits
                            (bin16SignifBits << SIGNIF_SHIFT));
        }

        assert -15 < bin16Exp && bin16Exp < 16;

        int floatExpBits = (bin16Exp + EXP_BIAS)
                << (SIGNIFICAND_WIDTH - 1);

        // Compute and combine result sign, exponent, and significand bits.
        return Float.intBitsToFloat((bin16SignBit << 16) |
                floatExpBits |
                (bin16SignifBits << SIGNIF_SHIFT));
    }

    /**
     * 单精度浮点数转换成二进制16位浮点数
     */
    public static short floatToFloat16(float f) {
        int doppel = Float.floatToRawIntBits(f);
        short sign_bit = (short) ((doppel & 0x8000_0000) >> 16);

        if (Float.isNaN(f)) {
            // Preserve sign and attempt to preserve significand bits
            return (short) (sign_bit
                    | 0x7c00 // max exponent + 1
                    // Preserve high order bit of float NaN in the
                    // binary16 result NaN (tenth bit); OR in remaining
                    // bits into lower 9 bits of binary 16 significand.
                    | (doppel & 0x007f_e000) >> 13 // 10 bits
                    | (doppel & 0x0000_1ff0) >> 4  //  9 bits
                    | (doppel & 0x0000_000f));     //  4 bits
        }

        float abs_f = Math.abs(f);

        // The overflow threshold is binary16 MAX_VALUE + 1/2 ulp
        if (abs_f >= (0x1.ffcp15f + 0x0.002p15f)) {
            return (short) (sign_bit | 0x7c00); // Positive or negative infinity
        }

        // Smallest magnitude nonzero representable binary16 value
        // is equal to 0x1.0p-24; half-way and smaller rounds to zero.
        if (abs_f <= 0x1.0p-24f * 0.5f) { // Covers float zeros and subnormals.
            return sign_bit; // Positive or negative zero
        }

        // Dealing with finite values in exponent range of binary16
        // (when rounding is done, could still round up)
        int exp = Math.getExponent(f);
        assert -25 <= exp && exp <= 15;

        // For binary16 subnormals, beside forcing exp to -15, retain
        // the difference expdelta = E_min - exp.  This is the excess
        // shift value, in addition to 13, to be used in the
        // computations below.  Further the (hidden) msb with value 1
        // in f must be involved as well.
        int expdelta = 0;
        int msb = 0x0000_0000;
        if (exp < -14) {
            expdelta = -14 - exp;
            exp = -15;
            msb = 0x0080_0000;
        }
        int f_signif_bits = doppel & 0x007f_ffff | msb;

        // Significand bits as if using rounding to zero (truncation).
        short signif_bits = (short) (f_signif_bits >> (13 + expdelta));

        // For round to nearest even, determining whether or not to
        // round up (in magnitude) is a function of the least
        // significant bit (LSB), the next bit position (the round
        // position), and the sticky bit (whether there are any
        // nonzero bits in the exact result to the right of the round
        // digit). An increment occurs in three cases:
        //
        // LSB  Round Sticky
        // 0    1     1
        // 1    1     0
        // 1    1     1
        // See "Computer Arithmetic Algorithms," Koren, Table 4.9

        int lsb = f_signif_bits & (1 << 13 + expdelta);
        int round = f_signif_bits & (1 << 12 + expdelta);
        int sticky = f_signif_bits & ((1 << 12 + expdelta) - 1);

        if (round != 0 && ((lsb | sticky) != 0)) {
            signif_bits++;
        }

        // No bits set in significand beyond the *first* exponent bit,
        // not just the sigificand; quantity is added to the exponent
        // to implement a carry out from rounding the significand.
        assert (0xf800 & signif_bits) == 0x0;

        return (short) (sign_bit | (((exp + 15) << 10) + signif_bits));
    }
}
