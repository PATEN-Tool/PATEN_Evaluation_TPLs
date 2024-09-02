// 
// Decompiled by Procyon v0.5.36
// 

package org.bouncycastle.math.ec;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import java.math.BigInteger;
import java.util.Hashtable;

public abstract class ECPoint
{
    protected static final ECFieldElement[] EMPTY_ZS;
    protected ECCurve curve;
    protected ECFieldElement x;
    protected ECFieldElement y;
    protected ECFieldElement[] zs;
    protected Hashtable preCompTable;
    
    protected static ECFieldElement[] getInitialZCoords(final ECCurve ecCurve) {
        final int n = (null == ecCurve) ? 0 : ecCurve.getCoordinateSystem();
        switch (n) {
            case 0:
            case 5: {
                return ECPoint.EMPTY_ZS;
            }
            default: {
                final ECFieldElement fromBigInteger = ecCurve.fromBigInteger(ECConstants.ONE);
                switch (n) {
                    case 1:
                    case 2:
                    case 6: {
                        return new ECFieldElement[] { fromBigInteger };
                    }
                    case 3: {
                        return new ECFieldElement[] { fromBigInteger, fromBigInteger, fromBigInteger };
                    }
                    case 4: {
                        return new ECFieldElement[] { fromBigInteger, ecCurve.getA() };
                    }
                    default: {
                        throw new IllegalArgumentException("unknown coordinate system");
                    }
                }
                break;
            }
        }
    }
    
    protected ECPoint(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
        this(ecCurve, ecFieldElement, ecFieldElement2, getInitialZCoords(ecCurve));
    }
    
    protected ECPoint(final ECCurve curve, final ECFieldElement x, final ECFieldElement y, final ECFieldElement[] zs) {
        this.preCompTable = null;
        this.curve = curve;
        this.x = x;
        this.y = y;
        this.zs = zs;
    }
    
    protected abstract boolean satisfiesCurveEquation();
    
    protected boolean satisfiesOrder() {
        if (ECConstants.ONE.equals(this.curve.getCofactor())) {
            return true;
        }
        final BigInteger order = this.curve.getOrder();
        return order == null || ECAlgorithms.referenceMultiply(this, order).isInfinity();
    }
    
    public final ECPoint getDetachedPoint() {
        return this.normalize().detach();
    }
    
    public ECCurve getCurve() {
        return this.curve;
    }
    
    protected abstract ECPoint detach();
    
    protected int getCurveCoordinateSystem() {
        return (null == this.curve) ? 0 : this.curve.getCoordinateSystem();
    }
    
    public ECFieldElement getAffineXCoord() {
        this.checkNormalized();
        return this.getXCoord();
    }
    
    public ECFieldElement getAffineYCoord() {
        this.checkNormalized();
        return this.getYCoord();
    }
    
    public ECFieldElement getXCoord() {
        return this.x;
    }
    
    public ECFieldElement getYCoord() {
        return this.y;
    }
    
    public ECFieldElement getZCoord(final int n) {
        return (n < 0 || n >= this.zs.length) ? null : this.zs[n];
    }
    
    public ECFieldElement[] getZCoords() {
        final int length = this.zs.length;
        if (length == 0) {
            return ECPoint.EMPTY_ZS;
        }
        final ECFieldElement[] array = new ECFieldElement[length];
        System.arraycopy(this.zs, 0, array, 0, length);
        return array;
    }
    
    public final ECFieldElement getRawXCoord() {
        return this.x;
    }
    
    public final ECFieldElement getRawYCoord() {
        return this.y;
    }
    
    protected final ECFieldElement[] getRawZCoords() {
        return this.zs;
    }
    
    protected void checkNormalized() {
        if (!this.isNormalized()) {
            throw new IllegalStateException("point not in normal form");
        }
    }
    
    public boolean isNormalized() {
        final int curveCoordinateSystem = this.getCurveCoordinateSystem();
        return curveCoordinateSystem == 0 || curveCoordinateSystem == 5 || this.isInfinity() || this.zs[0].isOne();
    }
    
    public ECPoint normalize() {
        if (this.isInfinity()) {
            return this;
        }
        switch (this.getCurveCoordinateSystem()) {
            case 0:
            case 5: {
                return this;
            }
            default: {
                final ECFieldElement zCoord = this.getZCoord(0);
                if (zCoord.isOne()) {
                    return this;
                }
                if (null == this.curve) {
                    throw new IllegalStateException("Detached points must be in affine coordinates");
                }
                final ECFieldElement randomFieldElementMult = this.curve.randomFieldElementMult(CryptoServicesRegistrar.getSecureRandom());
                return this.normalize(zCoord.multiply(randomFieldElementMult).invert().multiply(randomFieldElementMult));
            }
        }
    }
    
    ECPoint normalize(final ECFieldElement ecFieldElement) {
        switch (this.getCurveCoordinateSystem()) {
            case 1:
            case 6: {
                return this.createScaledPoint(ecFieldElement, ecFieldElement);
            }
            case 2:
            case 3:
            case 4: {
                final ECFieldElement square = ecFieldElement.square();
                return this.createScaledPoint(square, square.multiply(ecFieldElement));
            }
            default: {
                throw new IllegalStateException("not a projective coordinate system");
            }
        }
    }
    
    protected ECPoint createScaledPoint(final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
        return this.getCurve().createRawPoint(this.getRawXCoord().multiply(ecFieldElement), this.getRawYCoord().multiply(ecFieldElement2));
    }
    
    public boolean isInfinity() {
        return this.x == null || this.y == null || (this.zs.length > 0 && this.zs[0].isZero());
    }
    
    public boolean isValid() {
        return this.implIsValid(false, true);
    }
    
    boolean isValidPartial() {
        return this.implIsValid(false, false);
    }
    
    boolean implIsValid(final boolean b, final boolean b2) {
        return this.isInfinity() || !((ValidityPrecompInfo)this.getCurve().precompute(this, "bc_validity", new PreCompCallback() {
            public PreCompInfo precompute(final PreCompInfo preCompInfo) {
                ValidityPrecompInfo validityPrecompInfo = (preCompInfo instanceof ValidityPrecompInfo) ? ((ValidityPrecompInfo)preCompInfo) : null;
                if (validityPrecompInfo == null) {
                    validityPrecompInfo = new ValidityPrecompInfo();
                }
                if (validityPrecompInfo.hasFailed()) {
                    return validityPrecompInfo;
                }
                if (!validityPrecompInfo.hasCurveEquationPassed()) {
                    if (!b && !ECPoint.this.satisfiesCurveEquation()) {
                        validityPrecompInfo.reportFailed();
                        return validityPrecompInfo;
                    }
                    validityPrecompInfo.reportCurveEquationPassed();
                }
                if (b2 && !validityPrecompInfo.hasOrderPassed()) {
                    if (!ECPoint.this.satisfiesOrder()) {
                        validityPrecompInfo.reportFailed();
                        return validityPrecompInfo;
                    }
                    validityPrecompInfo.reportOrderPassed();
                }
                return validityPrecompInfo;
            }
        })).hasFailed();
    }
    
    public ECPoint scaleX(final ECFieldElement ecFieldElement) {
        return this.isInfinity() ? this : this.getCurve().createRawPoint(this.getRawXCoord().multiply(ecFieldElement), this.getRawYCoord(), this.getRawZCoords());
    }
    
    public ECPoint scaleXNegateY(final ECFieldElement ecFieldElement) {
        return this.isInfinity() ? this : this.getCurve().createRawPoint(this.getRawXCoord().multiply(ecFieldElement), this.getRawYCoord().negate(), this.getRawZCoords());
    }
    
    public ECPoint scaleY(final ECFieldElement ecFieldElement) {
        return this.isInfinity() ? this : this.getCurve().createRawPoint(this.getRawXCoord(), this.getRawYCoord().multiply(ecFieldElement), this.getRawZCoords());
    }
    
    public ECPoint scaleYNegateX(final ECFieldElement ecFieldElement) {
        return this.isInfinity() ? this : this.getCurve().createRawPoint(this.getRawXCoord().negate(), this.getRawYCoord().multiply(ecFieldElement), this.getRawZCoords());
    }
    
    public boolean equals(final ECPoint ecPoint) {
        if (null == ecPoint) {
            return false;
        }
        final ECCurve curve = this.getCurve();
        final ECCurve curve2 = ecPoint.getCurve();
        final boolean b = null == curve;
        final boolean b2 = null == curve2;
        final boolean infinity = this.isInfinity();
        final boolean infinity2 = ecPoint.isInfinity();
        if (infinity || infinity2) {
            return infinity && infinity2 && (b || b2 || curve.equals(curve2));
        }
        ECPoint normalize = this;
        ECPoint normalize2 = ecPoint;
        if (!b || !b2) {
            if (b) {
                normalize2 = normalize2.normalize();
            }
            else if (b2) {
                normalize = normalize.normalize();
            }
            else {
                if (!curve.equals(curve2)) {
                    return false;
                }
                final ECPoint[] array = { this, curve.importPoint(normalize2) };
                curve.normalizeAll(array);
                normalize = array[0];
                normalize2 = array[1];
            }
        }
        return normalize.getXCoord().equals(normalize2.getXCoord()) && normalize.getYCoord().equals(normalize2.getYCoord());
    }
    
    public boolean equals(final Object o) {
        return o == this || (o instanceof ECPoint && this.equals((ECPoint)o));
    }
    
    public int hashCode() {
        final ECCurve curve = this.getCurve();
        int n = (null == curve) ? 0 : (~curve.hashCode());
        if (!this.isInfinity()) {
            final ECPoint normalize = this.normalize();
            n = (n ^ normalize.getXCoord().hashCode() * 17 ^ normalize.getYCoord().hashCode() * 257);
        }
        return n;
    }
    
    public String toString() {
        if (this.isInfinity()) {
            return "INF";
        }
        final StringBuffer sb = new StringBuffer();
        sb.append('(');
        sb.append(this.getRawXCoord());
        sb.append(',');
        sb.append(this.getRawYCoord());
        for (int i = 0; i < this.zs.length; ++i) {
            sb.append(',');
            sb.append(this.zs[i]);
        }
        sb.append(')');
        return sb.toString();
    }
    
    public byte[] getEncoded(final boolean b) {
        if (this.isInfinity()) {
            return new byte[1];
        }
        final ECPoint normalize = this.normalize();
        final byte[] encoded = normalize.getXCoord().getEncoded();
        if (b) {
            final byte[] array = new byte[encoded.length + 1];
            array[0] = (byte)(normalize.getCompressionYTilde() ? 3 : 2);
            System.arraycopy(encoded, 0, array, 1, encoded.length);
            return array;
        }
        final byte[] encoded2 = normalize.getYCoord().getEncoded();
        final byte[] array2 = new byte[encoded.length + encoded2.length + 1];
        array2[0] = 4;
        System.arraycopy(encoded, 0, array2, 1, encoded.length);
        System.arraycopy(encoded2, 0, array2, encoded.length + 1, encoded2.length);
        return array2;
    }
    
    protected abstract boolean getCompressionYTilde();
    
    public abstract ECPoint add(final ECPoint p0);
    
    public abstract ECPoint negate();
    
    public abstract ECPoint subtract(final ECPoint p0);
    
    public ECPoint timesPow2(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("'e' cannot be negative");
        }
        ECPoint twice = this;
        while (--n >= 0) {
            twice = twice.twice();
        }
        return twice;
    }
    
    public abstract ECPoint twice();
    
    public ECPoint twicePlus(final ECPoint ecPoint) {
        return this.twice().add(ecPoint);
    }
    
    public ECPoint threeTimes() {
        return this.twicePlus(this);
    }
    
    public ECPoint multiply(final BigInteger bigInteger) {
        return this.getCurve().getMultiplier().multiply(this, bigInteger);
    }
    
    static {
        EMPTY_ZS = new ECFieldElement[0];
    }
    
    public static class F2m extends AbstractF2m
    {
        F2m(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
            super(ecCurve, ecFieldElement, ecFieldElement2);
        }
        
        F2m(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2, final ECFieldElement[] array) {
            super(ecCurve, ecFieldElement, ecFieldElement2, array);
        }
        
        protected ECPoint detach() {
            return new F2m(null, this.getAffineXCoord(), this.getAffineYCoord());
        }
        
        public ECFieldElement getYCoord() {
            final int curveCoordinateSystem = this.getCurveCoordinateSystem();
            switch (curveCoordinateSystem) {
                case 5:
                case 6: {
                    final ECFieldElement x = this.x;
                    final ECFieldElement y = this.y;
                    if (this.isInfinity() || x.isZero()) {
                        return y;
                    }
                    ECFieldElement ecFieldElement = y.add(x).multiply(x);
                    if (6 == curveCoordinateSystem) {
                        final ECFieldElement ecFieldElement2 = this.zs[0];
                        if (!ecFieldElement2.isOne()) {
                            ecFieldElement = ecFieldElement.divide(ecFieldElement2);
                        }
                    }
                    return ecFieldElement;
                }
                default: {
                    return this.y;
                }
            }
        }
        
        protected boolean getCompressionYTilde() {
            final ECFieldElement rawXCoord = this.getRawXCoord();
            if (rawXCoord.isZero()) {
                return false;
            }
            final ECFieldElement rawYCoord = this.getRawYCoord();
            switch (this.getCurveCoordinateSystem()) {
                case 5:
                case 6: {
                    return rawYCoord.testBitZero() != rawXCoord.testBitZero();
                }
                default: {
                    return rawYCoord.divide(rawXCoord).testBitZero();
                }
            }
        }
        
        public ECPoint add(final ECPoint ecPoint) {
            if (this.isInfinity()) {
                return ecPoint;
            }
            if (ecPoint.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            final int coordinateSystem = curve.getCoordinateSystem();
            final ECFieldElement x = this.x;
            final ECFieldElement x2 = ecPoint.x;
            switch (coordinateSystem) {
                case 0: {
                    final ECFieldElement y = this.y;
                    final ECFieldElement y2 = ecPoint.y;
                    final ECFieldElement add = x.add(x2);
                    final ECFieldElement add2 = y.add(y2);
                    if (!add.isZero()) {
                        final ECFieldElement divide = add2.divide(add);
                        final ECFieldElement add3 = divide.square().add(divide).add(add).add(curve.getA());
                        return new F2m(curve, add3, divide.multiply(x.add(add3)).add(add3).add(y));
                    }
                    if (add2.isZero()) {
                        return this.twice();
                    }
                    return curve.getInfinity();
                }
                case 1: {
                    final ECFieldElement y3 = this.y;
                    final ECFieldElement ecFieldElement = this.zs[0];
                    final ECFieldElement y4 = ecPoint.y;
                    final ECFieldElement ecFieldElement2 = ecPoint.zs[0];
                    final boolean one = ecFieldElement2.isOne();
                    final ECFieldElement add4 = ecFieldElement.multiply(y4).add(one ? y3 : y3.multiply(ecFieldElement2));
                    final ECFieldElement add5 = ecFieldElement.multiply(x2).add(one ? x : x.multiply(ecFieldElement2));
                    if (!add5.isZero()) {
                        final ECFieldElement square = add5.square();
                        final ECFieldElement multiply = square.multiply(add5);
                        final ECFieldElement ecFieldElement3 = one ? ecFieldElement : ecFieldElement.multiply(ecFieldElement2);
                        final ECFieldElement add6 = add4.add(add5);
                        final ECFieldElement add7 = add6.multiplyPlusProduct(add4, square, curve.getA()).multiply(ecFieldElement3).add(multiply);
                        return new F2m(curve, add5.multiply(add7), add4.multiplyPlusProduct(x, add5, y3).multiplyPlusProduct(one ? square : square.multiply(ecFieldElement2), add6, add7), new ECFieldElement[] { multiply.multiply(ecFieldElement3) });
                    }
                    if (add4.isZero()) {
                        return this.twice();
                    }
                    return curve.getInfinity();
                }
                case 6: {
                    if (x.isZero()) {
                        if (x2.isZero()) {
                            return curve.getInfinity();
                        }
                        return ecPoint.add(this);
                    }
                    else {
                        final ECFieldElement y5 = this.y;
                        final ECFieldElement ecFieldElement4 = this.zs[0];
                        final ECFieldElement y6 = ecPoint.y;
                        final ECFieldElement ecFieldElement5 = ecPoint.zs[0];
                        final boolean one2 = ecFieldElement4.isOne();
                        ECFieldElement multiply2 = x2;
                        ECFieldElement multiply3 = y6;
                        if (!one2) {
                            multiply2 = multiply2.multiply(ecFieldElement4);
                            multiply3 = multiply3.multiply(ecFieldElement4);
                        }
                        final boolean one3 = ecFieldElement5.isOne();
                        ECFieldElement multiply4 = x;
                        ECFieldElement multiply5 = y5;
                        if (!one3) {
                            multiply4 = multiply4.multiply(ecFieldElement5);
                            multiply5 = multiply5.multiply(ecFieldElement5);
                        }
                        final ECFieldElement add8 = multiply5.add(multiply3);
                        final ECFieldElement add9 = multiply4.add(multiply2);
                        if (!add9.isZero()) {
                            ECFieldElement ecFieldElement6;
                            ECFieldElement ecFieldElement7;
                            ECFieldElement ecFieldElement8;
                            if (x2.isZero()) {
                                final ECPoint normalize = this.normalize();
                                final ECFieldElement xCoord = normalize.getXCoord();
                                final ECFieldElement yCoord = normalize.getYCoord();
                                final ECFieldElement divide2 = yCoord.add(y6).divide(xCoord);
                                ecFieldElement6 = divide2.square().add(divide2).add(xCoord).add(curve.getA());
                                if (ecFieldElement6.isZero()) {
                                    return new F2m(curve, ecFieldElement6, curve.getB().sqrt());
                                }
                                ecFieldElement7 = divide2.multiply(xCoord.add(ecFieldElement6)).add(ecFieldElement6).add(yCoord).divide(ecFieldElement6).add(ecFieldElement6);
                                ecFieldElement8 = curve.fromBigInteger(ECConstants.ONE);
                            }
                            else {
                                final ECFieldElement square2 = add9.square();
                                final ECFieldElement multiply6 = add8.multiply(multiply4);
                                final ECFieldElement multiply7 = add8.multiply(multiply2);
                                ecFieldElement6 = multiply6.multiply(multiply7);
                                if (ecFieldElement6.isZero()) {
                                    return new F2m(curve, ecFieldElement6, curve.getB().sqrt());
                                }
                                ECFieldElement ecFieldElement9 = add8.multiply(square2);
                                if (!one3) {
                                    ecFieldElement9 = ecFieldElement9.multiply(ecFieldElement5);
                                }
                                ecFieldElement7 = multiply7.add(square2).squarePlusProduct(ecFieldElement9, y5.add(ecFieldElement4));
                                ecFieldElement8 = ecFieldElement9;
                                if (!one2) {
                                    ecFieldElement8 = ecFieldElement8.multiply(ecFieldElement4);
                                }
                            }
                            return new F2m(curve, ecFieldElement6, ecFieldElement7, new ECFieldElement[] { ecFieldElement8 });
                        }
                        if (add8.isZero()) {
                            return this.twice();
                        }
                        return curve.getInfinity();
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        public ECPoint twice() {
            if (this.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            final ECFieldElement x = this.x;
            if (x.isZero()) {
                return curve.getInfinity();
            }
            switch (curve.getCoordinateSystem()) {
                case 0: {
                    final ECFieldElement add = this.y.divide(x).add(x);
                    final ECFieldElement add2 = add.square().add(add).add(curve.getA());
                    return new F2m(curve, add2, x.squarePlusProduct(add2, add.addOne()));
                }
                case 1: {
                    final ECFieldElement y = this.y;
                    final ECFieldElement ecFieldElement = this.zs[0];
                    final boolean one = ecFieldElement.isOne();
                    final ECFieldElement ecFieldElement2 = one ? x : x.multiply(ecFieldElement);
                    final ECFieldElement ecFieldElement3 = one ? y : y.multiply(ecFieldElement);
                    final ECFieldElement square = x.square();
                    final ECFieldElement add3 = square.add(ecFieldElement3);
                    final ECFieldElement ecFieldElement4 = ecFieldElement2;
                    final ECFieldElement square2 = ecFieldElement4.square();
                    final ECFieldElement add4 = add3.add(ecFieldElement4);
                    final ECFieldElement multiplyPlusProduct = add4.multiplyPlusProduct(add3, square2, curve.getA());
                    return new F2m(curve, ecFieldElement4.multiply(multiplyPlusProduct), square.square().multiplyPlusProduct(ecFieldElement4, multiplyPlusProduct, add4), new ECFieldElement[] { ecFieldElement4.multiply(square2) });
                }
                case 6: {
                    final ECFieldElement y2 = this.y;
                    final ECFieldElement ecFieldElement5 = this.zs[0];
                    final boolean one2 = ecFieldElement5.isOne();
                    final ECFieldElement ecFieldElement6 = one2 ? y2 : y2.multiply(ecFieldElement5);
                    final ECFieldElement ecFieldElement7 = one2 ? ecFieldElement5 : ecFieldElement5.square();
                    final ECFieldElement a = curve.getA();
                    final ECFieldElement ecFieldElement8 = one2 ? a : a.multiply(ecFieldElement7);
                    final ECFieldElement add5 = y2.square().add(ecFieldElement6).add(ecFieldElement8);
                    if (add5.isZero()) {
                        return new F2m(curve, add5, curve.getB().sqrt());
                    }
                    final ECFieldElement square3 = add5.square();
                    final ECFieldElement ecFieldElement9 = one2 ? add5 : add5.multiply(ecFieldElement7);
                    final ECFieldElement b = curve.getB();
                    ECFieldElement ecFieldElement11;
                    if (b.bitLength() < curve.getFieldSize() >> 1) {
                        final ECFieldElement square4 = y2.add(x).square();
                        ECFieldElement ecFieldElement10;
                        if (b.isOne()) {
                            ecFieldElement10 = ecFieldElement8.add(ecFieldElement7).square();
                        }
                        else {
                            ecFieldElement10 = ecFieldElement8.squarePlusProduct(b, ecFieldElement7.square());
                        }
                        ecFieldElement11 = square4.add(add5).add(ecFieldElement7).multiply(square4).add(ecFieldElement10).add(square3);
                        if (a.isZero()) {
                            ecFieldElement11 = ecFieldElement11.add(ecFieldElement9);
                        }
                        else if (!a.isOne()) {
                            ecFieldElement11 = ecFieldElement11.add(a.addOne().multiply(ecFieldElement9));
                        }
                    }
                    else {
                        ecFieldElement11 = (one2 ? x : x.multiply(ecFieldElement5)).squarePlusProduct(add5, ecFieldElement6).add(square3).add(ecFieldElement9);
                    }
                    return new F2m(curve, square3, ecFieldElement11, new ECFieldElement[] { ecFieldElement9 });
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        public ECPoint twicePlus(final ECPoint ecPoint) {
            if (this.isInfinity()) {
                return ecPoint;
            }
            if (ecPoint.isInfinity()) {
                return this.twice();
            }
            final ECCurve curve = this.getCurve();
            final ECFieldElement x = this.x;
            if (x.isZero()) {
                return ecPoint;
            }
            switch (curve.getCoordinateSystem()) {
                case 6: {
                    final ECFieldElement x2 = ecPoint.x;
                    final ECFieldElement ecFieldElement = ecPoint.zs[0];
                    if (x2.isZero() || !ecFieldElement.isOne()) {
                        return this.twice().add(ecPoint);
                    }
                    final ECFieldElement y = this.y;
                    final ECFieldElement ecFieldElement2 = this.zs[0];
                    final ECFieldElement y2 = ecPoint.y;
                    final ECFieldElement square = x.square();
                    final ECFieldElement square2 = y.square();
                    final ECFieldElement square3 = ecFieldElement2.square();
                    final ECFieldElement add = curve.getA().multiply(square3).add(square2).add(y.multiply(ecFieldElement2));
                    final ECFieldElement addOne = y2.addOne();
                    final ECFieldElement multiplyPlusProduct = curve.getA().add(addOne).multiply(square3).add(square2).multiplyPlusProduct(add, square, square3);
                    final ECFieldElement multiply = x2.multiply(square3);
                    final ECFieldElement square4 = multiply.add(add).square();
                    if (square4.isZero()) {
                        if (multiplyPlusProduct.isZero()) {
                            return ecPoint.twice();
                        }
                        return curve.getInfinity();
                    }
                    else {
                        if (multiplyPlusProduct.isZero()) {
                            return new F2m(curve, multiplyPlusProduct, curve.getB().sqrt());
                        }
                        final ECFieldElement multiply2 = multiplyPlusProduct.square().multiply(multiply);
                        final ECFieldElement multiply3 = multiplyPlusProduct.multiply(square4).multiply(square3);
                        return new F2m(curve, multiply2, multiplyPlusProduct.add(square4).square().multiplyPlusProduct(add, addOne, multiply3), new ECFieldElement[] { multiply3 });
                    }
                    break;
                }
                default: {
                    return this.twice().add(ecPoint);
                }
            }
        }
        
        public ECPoint negate() {
            if (this.isInfinity()) {
                return this;
            }
            final ECFieldElement x = this.x;
            if (x.isZero()) {
                return this;
            }
            switch (this.getCurveCoordinateSystem()) {
                case 0: {
                    return new F2m(this.curve, x, this.y.add(x));
                }
                case 1: {
                    return new F2m(this.curve, x, this.y.add(x), new ECFieldElement[] { this.zs[0] });
                }
                case 5: {
                    return new F2m(this.curve, x, this.y.addOne());
                }
                case 6: {
                    final ECFieldElement y = this.y;
                    final ECFieldElement ecFieldElement = this.zs[0];
                    return new F2m(this.curve, x, y.add(ecFieldElement), new ECFieldElement[] { ecFieldElement });
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
    }
    
    public abstract static class AbstractF2m extends ECPoint
    {
        protected AbstractF2m(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
            super(ecCurve, ecFieldElement, ecFieldElement2);
        }
        
        protected AbstractF2m(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2, final ECFieldElement[] array) {
            super(ecCurve, ecFieldElement, ecFieldElement2, array);
        }
        
        protected boolean satisfiesCurveEquation() {
            final ECCurve curve = this.getCurve();
            final ECFieldElement x = this.x;
            ECFieldElement ecFieldElement = curve.getA();
            ECFieldElement ecFieldElement2 = curve.getB();
            final int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 6) {
                final ECFieldElement y = this.y;
                ECFieldElement ecFieldElement3 = y.add(x).multiply(y);
                switch (coordinateSystem) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        final ECFieldElement ecFieldElement4 = this.zs[0];
                        if (!ecFieldElement4.isOne()) {
                            final ECFieldElement multiply = ecFieldElement4.multiply(ecFieldElement4.square());
                            ecFieldElement3 = ecFieldElement3.multiply(ecFieldElement4);
                            ecFieldElement = ecFieldElement.multiply(ecFieldElement4);
                            ecFieldElement2 = ecFieldElement2.multiply(multiply);
                            break;
                        }
                        break;
                    }
                    default: {
                        throw new IllegalStateException("unsupported coordinate system");
                    }
                }
                return ecFieldElement3.equals(x.add(ecFieldElement).multiply(x.square()).add(ecFieldElement2));
            }
            final ECFieldElement ecFieldElement5 = this.zs[0];
            final boolean one = ecFieldElement5.isOne();
            if (x.isZero()) {
                final ECFieldElement square = this.y.square();
                ECFieldElement multiply2 = ecFieldElement2;
                if (!one) {
                    multiply2 = multiply2.multiply(ecFieldElement5.square());
                }
                return square.equals(multiply2);
            }
            final ECFieldElement y2 = this.y;
            final ECFieldElement square2 = x.square();
            ECFieldElement ecFieldElement6;
            ECFieldElement ecFieldElement7;
            if (one) {
                ecFieldElement6 = y2.square().add(y2).add(ecFieldElement);
                ecFieldElement7 = square2.square().add(ecFieldElement2);
            }
            else {
                final ECFieldElement square3 = ecFieldElement5.square();
                final ECFieldElement square4 = square3.square();
                ecFieldElement6 = y2.add(ecFieldElement5).multiplyPlusProduct(y2, ecFieldElement, square3);
                ecFieldElement7 = square2.squarePlusProduct(ecFieldElement2, square4);
            }
            return ecFieldElement6.multiply(square2).equals(ecFieldElement7);
        }
        
        protected boolean satisfiesOrder() {
            final BigInteger cofactor = this.curve.getCofactor();
            if (ECConstants.TWO.equals(cofactor)) {
                return 0 != ((ECFieldElement.AbstractF2m)this.normalize().getAffineXCoord()).trace();
            }
            if (ECConstants.FOUR.equals(cofactor)) {
                final ECPoint normalize = this.normalize();
                final ECFieldElement affineXCoord = normalize.getAffineXCoord();
                final ECFieldElement solveQuadraticEquation = ((ECCurve.AbstractF2m)this.curve).solveQuadraticEquation(affineXCoord.add(this.curve.getA()));
                return null != solveQuadraticEquation && 0 == ((ECFieldElement.AbstractF2m)affineXCoord.multiply(solveQuadraticEquation).add(normalize.getAffineYCoord())).trace();
            }
            return super.satisfiesOrder();
        }
        
        public ECPoint scaleX(final ECFieldElement ecFieldElement) {
            if (this.isInfinity()) {
                return this;
            }
            switch (this.getCurveCoordinateSystem()) {
                case 5: {
                    final ECFieldElement rawXCoord = this.getRawXCoord();
                    return this.getCurve().createRawPoint(rawXCoord, this.getRawYCoord().add(rawXCoord).divide(ecFieldElement).add(rawXCoord.multiply(ecFieldElement)), this.getRawZCoords());
                }
                case 6: {
                    final ECFieldElement rawXCoord2 = this.getRawXCoord();
                    final ECFieldElement rawYCoord = this.getRawYCoord();
                    final ECFieldElement ecFieldElement2 = this.getRawZCoords()[0];
                    final ECFieldElement multiply = rawXCoord2.multiply(ecFieldElement.square());
                    return this.getCurve().createRawPoint(multiply, rawYCoord.add(rawXCoord2).add(multiply), new ECFieldElement[] { ecFieldElement2.multiply(ecFieldElement) });
                }
                default: {
                    return super.scaleX(ecFieldElement);
                }
            }
        }
        
        public ECPoint scaleXNegateY(final ECFieldElement ecFieldElement) {
            return this.scaleX(ecFieldElement);
        }
        
        public ECPoint scaleY(final ECFieldElement ecFieldElement) {
            if (this.isInfinity()) {
                return this;
            }
            switch (this.getCurveCoordinateSystem()) {
                case 5:
                case 6: {
                    final ECFieldElement rawXCoord = this.getRawXCoord();
                    return this.getCurve().createRawPoint(rawXCoord, this.getRawYCoord().add(rawXCoord).multiply(ecFieldElement).add(rawXCoord), this.getRawZCoords());
                }
                default: {
                    return super.scaleY(ecFieldElement);
                }
            }
        }
        
        public ECPoint scaleYNegateX(final ECFieldElement ecFieldElement) {
            return this.scaleY(ecFieldElement);
        }
        
        public ECPoint subtract(final ECPoint ecPoint) {
            if (ecPoint.isInfinity()) {
                return this;
            }
            return this.add(ecPoint.negate());
        }
        
        public AbstractF2m tau() {
            if (this.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            final int coordinateSystem = curve.getCoordinateSystem();
            final ECFieldElement x = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5: {
                    return (AbstractF2m)curve.createRawPoint(x.square(), this.y.square());
                }
                case 1:
                case 6: {
                    return (AbstractF2m)curve.createRawPoint(x.square(), this.y.square(), new ECFieldElement[] { this.zs[0].square() });
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        public AbstractF2m tauPow(final int n) {
            if (this.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            final int coordinateSystem = curve.getCoordinateSystem();
            final ECFieldElement x = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5: {
                    return (AbstractF2m)curve.createRawPoint(x.squarePow(n), this.y.squarePow(n));
                }
                case 1:
                case 6: {
                    return (AbstractF2m)curve.createRawPoint(x.squarePow(n), this.y.squarePow(n), new ECFieldElement[] { this.zs[0].squarePow(n) });
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
    }
    
    public static class Fp extends AbstractFp
    {
        Fp(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
            super(ecCurve, ecFieldElement, ecFieldElement2);
        }
        
        Fp(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2, final ECFieldElement[] array) {
            super(ecCurve, ecFieldElement, ecFieldElement2, array);
        }
        
        protected ECPoint detach() {
            return new Fp(null, this.getAffineXCoord(), this.getAffineYCoord());
        }
        
        public ECFieldElement getZCoord(final int n) {
            if (n == 1 && 4 == this.getCurveCoordinateSystem()) {
                return this.getJacobianModifiedW();
            }
            return super.getZCoord(n);
        }
        
        public ECPoint add(final ECPoint ecPoint) {
            if (this.isInfinity()) {
                return ecPoint;
            }
            if (ecPoint.isInfinity()) {
                return this;
            }
            if (this == ecPoint) {
                return this.twice();
            }
            final ECCurve curve = this.getCurve();
            final int coordinateSystem = curve.getCoordinateSystem();
            final ECFieldElement x = this.x;
            final ECFieldElement y = this.y;
            final ECFieldElement x2 = ecPoint.x;
            final ECFieldElement y2 = ecPoint.y;
            switch (coordinateSystem) {
                case 0: {
                    final ECFieldElement subtract = x2.subtract(x);
                    final ECFieldElement subtract2 = y2.subtract(y);
                    if (!subtract.isZero()) {
                        final ECFieldElement divide = subtract2.divide(subtract);
                        final ECFieldElement subtract3 = divide.square().subtract(x).subtract(x2);
                        return new Fp(curve, subtract3, divide.multiply(x.subtract(subtract3)).subtract(y));
                    }
                    if (subtract2.isZero()) {
                        return this.twice();
                    }
                    return curve.getInfinity();
                }
                case 1: {
                    final ECFieldElement ecFieldElement = this.zs[0];
                    final ECFieldElement ecFieldElement2 = ecPoint.zs[0];
                    final boolean one = ecFieldElement.isOne();
                    final boolean one2 = ecFieldElement2.isOne();
                    final ECFieldElement ecFieldElement3 = one ? y2 : y2.multiply(ecFieldElement);
                    final ECFieldElement ecFieldElement4 = one2 ? y : y.multiply(ecFieldElement2);
                    final ECFieldElement subtract4 = ecFieldElement3.subtract(ecFieldElement4);
                    final ECFieldElement ecFieldElement5 = one ? x2 : x2.multiply(ecFieldElement);
                    final ECFieldElement ecFieldElement6 = one2 ? x : x.multiply(ecFieldElement2);
                    final ECFieldElement subtract5 = ecFieldElement5.subtract(ecFieldElement6);
                    if (!subtract5.isZero()) {
                        final ECFieldElement ecFieldElement7 = one ? ecFieldElement2 : (one2 ? ecFieldElement : ecFieldElement.multiply(ecFieldElement2));
                        final ECFieldElement square = subtract5.square();
                        final ECFieldElement multiply = square.multiply(subtract5);
                        final ECFieldElement multiply2 = square.multiply(ecFieldElement6);
                        final ECFieldElement subtract6 = subtract4.square().multiply(ecFieldElement7).subtract(multiply).subtract(this.two(multiply2));
                        return new Fp(curve, subtract5.multiply(subtract6), multiply2.subtract(subtract6).multiplyMinusProduct(subtract4, ecFieldElement4, multiply), new ECFieldElement[] { multiply.multiply(ecFieldElement7) });
                    }
                    if (subtract4.isZero()) {
                        return this.twice();
                    }
                    return curve.getInfinity();
                }
                case 2:
                case 4: {
                    final ECFieldElement ecFieldElement8 = this.zs[0];
                    final ECFieldElement ecFieldElement9 = ecPoint.zs[0];
                    final boolean one3 = ecFieldElement8.isOne();
                    ECFieldElement ecFieldElement10 = null;
                    ECFieldElement ecFieldElement11;
                    ECFieldElement ecFieldElement12;
                    ECFieldElement ecFieldElement13;
                    if (!one3 && ecFieldElement8.equals(ecFieldElement9)) {
                        final ECFieldElement subtract7 = x.subtract(x2);
                        final ECFieldElement subtract8 = y.subtract(y2);
                        if (subtract7.isZero()) {
                            if (subtract8.isZero()) {
                                return this.twice();
                            }
                            return curve.getInfinity();
                        }
                        else {
                            final ECFieldElement square2 = subtract7.square();
                            final ECFieldElement multiply3 = x.multiply(square2);
                            final ECFieldElement multiply4 = x2.multiply(square2);
                            final ECFieldElement multiply5 = multiply3.subtract(multiply4).multiply(y);
                            ecFieldElement11 = subtract8.square().subtract(multiply3).subtract(multiply4);
                            ecFieldElement12 = multiply3.subtract(ecFieldElement11).multiply(subtract8).subtract(multiply5);
                            ecFieldElement13 = subtract7.multiply(ecFieldElement8);
                        }
                    }
                    else {
                        ECFieldElement multiply6;
                        ECFieldElement multiply7;
                        if (one3) {
                            multiply6 = x2;
                            multiply7 = y2;
                        }
                        else {
                            final ECFieldElement square3 = ecFieldElement8.square();
                            multiply6 = square3.multiply(x2);
                            multiply7 = square3.multiply(ecFieldElement8).multiply(y2);
                        }
                        final boolean one4 = ecFieldElement9.isOne();
                        ECFieldElement multiply8;
                        ECFieldElement multiply9;
                        if (one4) {
                            multiply8 = x;
                            multiply9 = y;
                        }
                        else {
                            final ECFieldElement square4 = ecFieldElement9.square();
                            multiply8 = square4.multiply(x);
                            multiply9 = square4.multiply(ecFieldElement9).multiply(y);
                        }
                        final ECFieldElement subtract9 = multiply8.subtract(multiply6);
                        final ECFieldElement subtract10 = multiply9.subtract(multiply7);
                        if (subtract9.isZero()) {
                            if (subtract10.isZero()) {
                                return this.twice();
                            }
                            return curve.getInfinity();
                        }
                        else {
                            final ECFieldElement square5 = subtract9.square();
                            final ECFieldElement multiply10 = square5.multiply(subtract9);
                            final ECFieldElement multiply11 = square5.multiply(multiply8);
                            ecFieldElement11 = subtract10.square().add(multiply10).subtract(this.two(multiply11));
                            ecFieldElement12 = multiply11.subtract(ecFieldElement11).multiplyMinusProduct(subtract10, multiply10, multiply9);
                            ecFieldElement13 = subtract9;
                            if (!one3) {
                                ecFieldElement13 = ecFieldElement13.multiply(ecFieldElement8);
                            }
                            if (!one4) {
                                ecFieldElement13 = ecFieldElement13.multiply(ecFieldElement9);
                            }
                            if (ecFieldElement13 == subtract9) {
                                ecFieldElement10 = square5;
                            }
                        }
                    }
                    ECFieldElement[] array;
                    if (coordinateSystem == 4) {
                        array = new ECFieldElement[] { ecFieldElement13, this.calculateJacobianModifiedW(ecFieldElement13, ecFieldElement10) };
                    }
                    else {
                        array = new ECFieldElement[] { ecFieldElement13 };
                    }
                    return new Fp(curve, ecFieldElement11, ecFieldElement12, array);
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        public ECPoint twice() {
            if (this.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            final ECFieldElement y = this.y;
            if (y.isZero()) {
                return curve.getInfinity();
            }
            final int coordinateSystem = curve.getCoordinateSystem();
            final ECFieldElement x = this.x;
            switch (coordinateSystem) {
                case 0: {
                    final ECFieldElement divide = this.three(x.square()).add(this.getCurve().getA()).divide(this.two(y));
                    final ECFieldElement subtract = divide.square().subtract(this.two(x));
                    return new Fp(curve, subtract, divide.multiply(x.subtract(subtract)).subtract(y));
                }
                case 1: {
                    final ECFieldElement ecFieldElement = this.zs[0];
                    final boolean one = ecFieldElement.isOne();
                    ECFieldElement ecFieldElement2 = curve.getA();
                    if (!ecFieldElement2.isZero() && !one) {
                        ecFieldElement2 = ecFieldElement2.multiply(ecFieldElement.square());
                    }
                    final ECFieldElement add = ecFieldElement2.add(this.three(x.square()));
                    final ECFieldElement ecFieldElement3 = one ? y : y.multiply(ecFieldElement);
                    final ECFieldElement ecFieldElement4 = one ? y.square() : ecFieldElement3.multiply(y);
                    final ECFieldElement four = this.four(x.multiply(ecFieldElement4));
                    final ECFieldElement subtract2 = add.square().subtract(this.two(four));
                    final ECFieldElement two = this.two(ecFieldElement3);
                    final ECFieldElement multiply = subtract2.multiply(two);
                    final ECFieldElement two2 = this.two(ecFieldElement4);
                    return new Fp(curve, multiply, four.subtract(subtract2).multiply(add).subtract(this.two(two2.square())), new ECFieldElement[] { this.two(one ? this.two(two2) : two.square()).multiply(ecFieldElement3) });
                }
                case 2: {
                    final ECFieldElement ecFieldElement5 = this.zs[0];
                    final boolean one2 = ecFieldElement5.isOne();
                    final ECFieldElement square = y.square();
                    final ECFieldElement square2 = square.square();
                    final ECFieldElement a = curve.getA();
                    final ECFieldElement negate = a.negate();
                    ECFieldElement ecFieldElement7;
                    ECFieldElement ecFieldElement8;
                    if (negate.toBigInteger().equals(BigInteger.valueOf(3L))) {
                        final ECFieldElement ecFieldElement6 = one2 ? ecFieldElement5 : ecFieldElement5.square();
                        ecFieldElement7 = this.three(x.add(ecFieldElement6).multiply(x.subtract(ecFieldElement6)));
                        ecFieldElement8 = this.four(square.multiply(x));
                    }
                    else {
                        ecFieldElement7 = this.three(x.square());
                        if (one2) {
                            ecFieldElement7 = ecFieldElement7.add(a);
                        }
                        else if (!a.isZero()) {
                            final ECFieldElement square3 = ecFieldElement5.square().square();
                            if (negate.bitLength() < a.bitLength()) {
                                ecFieldElement7 = ecFieldElement7.subtract(square3.multiply(negate));
                            }
                            else {
                                ecFieldElement7 = ecFieldElement7.add(square3.multiply(a));
                            }
                        }
                        ecFieldElement8 = this.four(x.multiply(square));
                    }
                    final ECFieldElement subtract3 = ecFieldElement7.square().subtract(this.two(ecFieldElement8));
                    final ECFieldElement subtract4 = ecFieldElement8.subtract(subtract3).multiply(ecFieldElement7).subtract(this.eight(square2));
                    ECFieldElement ecFieldElement9 = this.two(y);
                    if (!one2) {
                        ecFieldElement9 = ecFieldElement9.multiply(ecFieldElement5);
                    }
                    return new Fp(curve, subtract3, subtract4, new ECFieldElement[] { ecFieldElement9 });
                }
                case 4: {
                    return this.twiceJacobianModified(true);
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        public ECPoint twicePlus(final ECPoint ecPoint) {
            if (this == ecPoint) {
                return this.threeTimes();
            }
            if (this.isInfinity()) {
                return ecPoint;
            }
            if (ecPoint.isInfinity()) {
                return this.twice();
            }
            final ECFieldElement y = this.y;
            if (y.isZero()) {
                return ecPoint;
            }
            final ECCurve curve = this.getCurve();
            switch (curve.getCoordinateSystem()) {
                case 0: {
                    final ECFieldElement x = this.x;
                    final ECFieldElement x2 = ecPoint.x;
                    final ECFieldElement y2 = ecPoint.y;
                    final ECFieldElement subtract = x2.subtract(x);
                    final ECFieldElement subtract2 = y2.subtract(y);
                    if (subtract.isZero()) {
                        if (subtract2.isZero()) {
                            return this.threeTimes();
                        }
                        return this;
                    }
                    else {
                        final ECFieldElement square = subtract.square();
                        final ECFieldElement subtract3 = square.multiply(this.two(x).add(x2)).subtract(subtract2.square());
                        if (subtract3.isZero()) {
                            return curve.getInfinity();
                        }
                        final ECFieldElement invert = subtract3.multiply(subtract).invert();
                        final ECFieldElement multiply = subtract3.multiply(invert).multiply(subtract2);
                        final ECFieldElement subtract4 = this.two(y).multiply(square).multiply(subtract).multiply(invert).subtract(multiply);
                        final ECFieldElement add = subtract4.subtract(multiply).multiply(multiply.add(subtract4)).add(x2);
                        return new Fp(curve, add, x.subtract(add).multiply(subtract4).subtract(y));
                    }
                    break;
                }
                case 4: {
                    return this.twiceJacobianModified(false).add(ecPoint);
                }
                default: {
                    return this.twice().add(ecPoint);
                }
            }
        }
        
        public ECPoint threeTimes() {
            if (this.isInfinity()) {
                return this;
            }
            final ECFieldElement y = this.y;
            if (y.isZero()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            switch (curve.getCoordinateSystem()) {
                case 0: {
                    final ECFieldElement x = this.x;
                    final ECFieldElement two = this.two(y);
                    final ECFieldElement square = two.square();
                    final ECFieldElement add = this.three(x.square()).add(this.getCurve().getA());
                    final ECFieldElement subtract = this.three(x).multiply(square).subtract(add.square());
                    if (subtract.isZero()) {
                        return this.getCurve().getInfinity();
                    }
                    final ECFieldElement invert = subtract.multiply(two).invert();
                    final ECFieldElement multiply = subtract.multiply(invert).multiply(add);
                    final ECFieldElement subtract2 = square.square().multiply(invert).subtract(multiply);
                    final ECFieldElement add2 = subtract2.subtract(multiply).multiply(multiply.add(subtract2)).add(x);
                    return new Fp(curve, add2, x.subtract(add2).multiply(subtract2).subtract(y));
                }
                case 4: {
                    return this.twiceJacobianModified(false).add(this);
                }
                default: {
                    return this.twice().add(this);
                }
            }
        }
        
        public ECPoint timesPow2(final int n) {
            if (n < 0) {
                throw new IllegalArgumentException("'e' cannot be negative");
            }
            if (n == 0 || this.isInfinity()) {
                return this;
            }
            if (n == 1) {
                return this.twice();
            }
            final ECCurve curve = this.getCurve();
            ECFieldElement ecFieldElement = this.y;
            if (ecFieldElement.isZero()) {
                return curve.getInfinity();
            }
            final int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement ecFieldElement2 = curve.getA();
            ECFieldElement ecFieldElement3 = this.x;
            ECFieldElement ecFieldElement4 = (this.zs.length < 1) ? curve.fromBigInteger(ECConstants.ONE) : this.zs[0];
            if (!ecFieldElement4.isOne()) {
                switch (coordinateSystem) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        final ECFieldElement square = ecFieldElement4.square();
                        ecFieldElement3 = ecFieldElement3.multiply(ecFieldElement4);
                        ecFieldElement = ecFieldElement.multiply(square);
                        ecFieldElement2 = this.calculateJacobianModifiedW(ecFieldElement4, square);
                        break;
                    }
                    case 2: {
                        ecFieldElement2 = this.calculateJacobianModifiedW(ecFieldElement4, null);
                        break;
                    }
                    case 4: {
                        ecFieldElement2 = this.getJacobianModifiedW();
                        break;
                    }
                    default: {
                        throw new IllegalStateException("unsupported coordinate system");
                    }
                }
            }
            for (int i = 0; i < n; ++i) {
                if (ecFieldElement.isZero()) {
                    return curve.getInfinity();
                }
                ECFieldElement ecFieldElement5 = this.three(ecFieldElement3.square());
                final ECFieldElement two = this.two(ecFieldElement);
                final ECFieldElement multiply = two.multiply(ecFieldElement);
                final ECFieldElement two2 = this.two(ecFieldElement3.multiply(multiply));
                final ECFieldElement two3 = this.two(multiply.square());
                if (!ecFieldElement2.isZero()) {
                    ecFieldElement5 = ecFieldElement5.add(ecFieldElement2);
                    ecFieldElement2 = this.two(two3.multiply(ecFieldElement2));
                }
                ecFieldElement3 = ecFieldElement5.square().subtract(this.two(two2));
                ecFieldElement = ecFieldElement5.multiply(two2.subtract(ecFieldElement3)).subtract(two3);
                ecFieldElement4 = (ecFieldElement4.isOne() ? two : two.multiply(ecFieldElement4));
            }
            switch (coordinateSystem) {
                case 0: {
                    final ECFieldElement invert = ecFieldElement4.invert();
                    final ECFieldElement square2 = invert.square();
                    return new Fp(curve, ecFieldElement3.multiply(square2), ecFieldElement.multiply(square2.multiply(invert)));
                }
                case 1: {
                    return new Fp(curve, ecFieldElement3.multiply(ecFieldElement4), ecFieldElement, new ECFieldElement[] { ecFieldElement4.multiply(ecFieldElement4.square()) });
                }
                case 2: {
                    return new Fp(curve, ecFieldElement3, ecFieldElement, new ECFieldElement[] { ecFieldElement4 });
                }
                case 4: {
                    return new Fp(curve, ecFieldElement3, ecFieldElement, new ECFieldElement[] { ecFieldElement4, ecFieldElement2 });
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
        }
        
        protected ECFieldElement two(final ECFieldElement ecFieldElement) {
            return ecFieldElement.add(ecFieldElement);
        }
        
        protected ECFieldElement three(final ECFieldElement ecFieldElement) {
            return this.two(ecFieldElement).add(ecFieldElement);
        }
        
        protected ECFieldElement four(final ECFieldElement ecFieldElement) {
            return this.two(this.two(ecFieldElement));
        }
        
        protected ECFieldElement eight(final ECFieldElement ecFieldElement) {
            return this.four(this.two(ecFieldElement));
        }
        
        protected ECFieldElement doubleProductFromSquares(final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2, final ECFieldElement ecFieldElement3, final ECFieldElement ecFieldElement4) {
            return ecFieldElement.add(ecFieldElement2).square().subtract(ecFieldElement3).subtract(ecFieldElement4);
        }
        
        public ECPoint negate() {
            if (this.isInfinity()) {
                return this;
            }
            final ECCurve curve = this.getCurve();
            if (0 != curve.getCoordinateSystem()) {
                return new Fp(curve, this.x, this.y.negate(), this.zs);
            }
            return new Fp(curve, this.x, this.y.negate());
        }
        
        protected ECFieldElement calculateJacobianModifiedW(final ECFieldElement ecFieldElement, ECFieldElement square) {
            final ECFieldElement a = this.getCurve().getA();
            if (a.isZero() || ecFieldElement.isOne()) {
                return a;
            }
            if (square == null) {
                square = ecFieldElement.square();
            }
            final ECFieldElement square2 = square.square();
            final ECFieldElement negate = a.negate();
            ECFieldElement ecFieldElement2;
            if (negate.bitLength() < a.bitLength()) {
                ecFieldElement2 = square2.multiply(negate).negate();
            }
            else {
                ecFieldElement2 = square2.multiply(a);
            }
            return ecFieldElement2;
        }
        
        protected ECFieldElement getJacobianModifiedW() {
            ECFieldElement ecFieldElement = this.zs[1];
            if (ecFieldElement == null) {
                ecFieldElement = (this.zs[1] = this.calculateJacobianModifiedW(this.zs[0], null));
            }
            return ecFieldElement;
        }
        
        protected Fp twiceJacobianModified(final boolean b) {
            final ECFieldElement x = this.x;
            final ECFieldElement y = this.y;
            final ECFieldElement ecFieldElement = this.zs[0];
            final ECFieldElement jacobianModifiedW = this.getJacobianModifiedW();
            final ECFieldElement add = this.three(x.square()).add(jacobianModifiedW);
            final ECFieldElement two = this.two(y);
            final ECFieldElement multiply = two.multiply(y);
            final ECFieldElement two2 = this.two(x.multiply(multiply));
            final ECFieldElement subtract = add.square().subtract(this.two(two2));
            final ECFieldElement two3 = this.two(multiply.square());
            return new Fp(this.getCurve(), subtract, add.multiply(two2.subtract(subtract)).subtract(two3), new ECFieldElement[] { ecFieldElement.isOne() ? two : two.multiply(ecFieldElement), b ? this.two(two3.multiply(jacobianModifiedW)) : null });
        }
    }
    
    public abstract static class AbstractFp extends ECPoint
    {
        protected AbstractFp(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2) {
            super(ecCurve, ecFieldElement, ecFieldElement2);
        }
        
        protected AbstractFp(final ECCurve ecCurve, final ECFieldElement ecFieldElement, final ECFieldElement ecFieldElement2, final ECFieldElement[] array) {
            super(ecCurve, ecFieldElement, ecFieldElement2, array);
        }
        
        protected boolean getCompressionYTilde() {
            return this.getAffineYCoord().testBitZero();
        }
        
        protected boolean satisfiesCurveEquation() {
            final ECFieldElement x = this.x;
            final ECFieldElement y = this.y;
            ECFieldElement ecFieldElement = this.curve.getA();
            ECFieldElement ecFieldElement2 = this.curve.getB();
            ECFieldElement ecFieldElement3 = y.square();
            switch (this.getCurveCoordinateSystem()) {
                case 0: {
                    break;
                }
                case 1: {
                    final ECFieldElement ecFieldElement4 = this.zs[0];
                    if (!ecFieldElement4.isOne()) {
                        final ECFieldElement square = ecFieldElement4.square();
                        final ECFieldElement multiply = ecFieldElement4.multiply(square);
                        ecFieldElement3 = ecFieldElement3.multiply(ecFieldElement4);
                        ecFieldElement = ecFieldElement.multiply(square);
                        ecFieldElement2 = ecFieldElement2.multiply(multiply);
                        break;
                    }
                    break;
                }
                case 2:
                case 3:
                case 4: {
                    final ECFieldElement ecFieldElement5 = this.zs[0];
                    if (!ecFieldElement5.isOne()) {
                        final ECFieldElement square2 = ecFieldElement5.square();
                        final ECFieldElement square3 = square2.square();
                        final ECFieldElement multiply2 = square2.multiply(square3);
                        ecFieldElement = ecFieldElement.multiply(square3);
                        ecFieldElement2 = ecFieldElement2.multiply(multiply2);
                        break;
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("unsupported coordinate system");
                }
            }
            return ecFieldElement3.equals(x.square().add(ecFieldElement).multiply(x).add(ecFieldElement2));
        }
        
        public ECPoint subtract(final ECPoint ecPoint) {
            if (ecPoint.isInfinity()) {
                return this;
            }
            return this.add(ecPoint.negate());
        }
    }
}
