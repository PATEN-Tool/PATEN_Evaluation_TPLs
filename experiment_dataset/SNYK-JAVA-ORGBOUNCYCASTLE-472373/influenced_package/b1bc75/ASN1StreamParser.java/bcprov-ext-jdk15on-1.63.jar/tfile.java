// 
// Decompiled by Procyon v0.5.36
// 

package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ASN1StreamParser
{
    private final InputStream _in;
    private final int _limit;
    private final byte[][] tmpBuffers;
    
    public ASN1StreamParser(final InputStream inputStream) {
        this(inputStream, StreamUtil.findLimit(inputStream));
    }
    
    public ASN1StreamParser(final InputStream in, final int limit) {
        this._in = in;
        this._limit = limit;
        this.tmpBuffers = new byte[11][];
    }
    
    public ASN1StreamParser(final byte[] array) {
        this(new ByteArrayInputStream(array), array.length);
    }
    
    ASN1Encodable readIndef(final int n) throws IOException {
        switch (n) {
            case 8: {
                return new DERExternalParser(this);
            }
            case 4: {
                return new BEROctetStringParser(this);
            }
            case 16: {
                return new BERSequenceParser(this);
            }
            case 17: {
                return new BERSetParser(this);
            }
            default: {
                throw new ASN1Exception("unknown BER object encountered: 0x" + Integer.toHexString(n));
            }
        }
    }
    
    ASN1Encodable readImplicit(final boolean b, final int n) throws IOException {
        if (!(this._in instanceof IndefiniteLengthInputStream)) {
            if (b) {
                switch (n) {
                    case 17: {
                        return new DLSetParser(this);
                    }
                    case 16: {
                        return new DLSequenceParser(this);
                    }
                    case 4: {
                        return new BEROctetStringParser(this);
                    }
                }
            }
            else {
                switch (n) {
                    case 17: {
                        throw new ASN1Exception("sequences must use constructed encoding (see X.690 8.9.1/8.10.1)");
                    }
                    case 16: {
                        throw new ASN1Exception("sets must use constructed encoding (see X.690 8.11.1/8.12.1)");
                    }
                    case 4: {
                        return new DEROctetStringParser((DefiniteLengthInputStream)this._in);
                    }
                }
            }
            throw new ASN1Exception("implicit tagging not implemented");
        }
        if (!b) {
            throw new IOException("indefinite-length primitive encoding encountered");
        }
        return this.readIndef(n);
    }
    
    ASN1Primitive readTaggedObject(final boolean b, final int n) throws IOException {
        if (!b) {
            return new DLTaggedObject(false, n, new DEROctetString(((DefiniteLengthInputStream)this._in).toByteArray()));
        }
        final ASN1EncodableVector vector = this.readVector();
        if (this._in instanceof IndefiniteLengthInputStream) {
            return (vector.size() == 1) ? new BERTaggedObject(true, n, vector.get(0)) : new BERTaggedObject(false, n, BERFactory.createSequence(vector));
        }
        return (vector.size() == 1) ? new DLTaggedObject(true, n, vector.get(0)) : new DLTaggedObject(false, n, DLFactory.createSequence(vector));
    }
    
    public ASN1Encodable readObject() throws IOException {
        final int read = this._in.read();
        if (read == -1) {
            return null;
        }
        this.set00Check(false);
        final int tagNumber = ASN1InputStream.readTagNumber(this._in, read);
        final boolean b = (read & 0x20) != 0x0;
        final int length = ASN1InputStream.readLength(this._in, this._limit, tagNumber == 4 || tagNumber == 16 || tagNumber == 17 || tagNumber == 8);
        if (length < 0) {
            if (!b) {
                throw new IOException("indefinite-length primitive encoding encountered");
            }
            final ASN1StreamParser asn1StreamParser = new ASN1StreamParser(new IndefiniteLengthInputStream(this._in, this._limit), this._limit);
            if ((read & 0x40) != 0x0) {
                return new BERApplicationSpecificParser(tagNumber, asn1StreamParser);
            }
            if ((read & 0x80) != 0x0) {
                return new BERTaggedObjectParser(true, tagNumber, asn1StreamParser);
            }
            return asn1StreamParser.readIndef(tagNumber);
        }
        else {
            final DefiniteLengthInputStream definiteLengthInputStream = new DefiniteLengthInputStream(this._in, length);
            if ((read & 0x40) != 0x0) {
                return new DLApplicationSpecific(b, tagNumber, definiteLengthInputStream.toByteArray());
            }
            if ((read & 0x80) != 0x0) {
                return new BERTaggedObjectParser(b, tagNumber, new ASN1StreamParser(definiteLengthInputStream));
            }
            if (b) {
                switch (tagNumber) {
                    case 4: {
                        return new BEROctetStringParser(new ASN1StreamParser(definiteLengthInputStream));
                    }
                    case 16: {
                        return new DLSequenceParser(new ASN1StreamParser(definiteLengthInputStream));
                    }
                    case 17: {
                        return new DLSetParser(new ASN1StreamParser(definiteLengthInputStream));
                    }
                    case 8: {
                        return new DERExternalParser(new ASN1StreamParser(definiteLengthInputStream));
                    }
                    default: {
                        throw new IOException("unknown tag " + tagNumber + " encountered");
                    }
                }
            }
            else {
                switch (tagNumber) {
                    case 4: {
                        return new DEROctetStringParser(definiteLengthInputStream);
                    }
                    default: {
                        try {
                            return ASN1InputStream.createPrimitiveDERObject(tagNumber, definiteLengthInputStream, this.tmpBuffers);
                        }
                        catch (IllegalArgumentException ex) {
                            throw new ASN1Exception("corrupted stream detected", ex);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void set00Check(final boolean eofOn00) {
        if (this._in instanceof IndefiniteLengthInputStream) {
            ((IndefiniteLengthInputStream)this._in).setEofOn00(eofOn00);
        }
    }
    
    ASN1EncodableVector readVector() throws IOException {
        ASN1Encodable asn1Encodable = this.readObject();
        if (null == asn1Encodable) {
            return new ASN1EncodableVector(0);
        }
        final ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        do {
            if (asn1Encodable instanceof InMemoryRepresentable) {
                asn1EncodableVector.add(((InMemoryRepresentable)asn1Encodable).getLoadedObject());
            }
            else {
                asn1EncodableVector.add(asn1Encodable.toASN1Primitive());
            }
        } while ((asn1Encodable = this.readObject()) != null);
        return asn1EncodableVector;
    }
}
