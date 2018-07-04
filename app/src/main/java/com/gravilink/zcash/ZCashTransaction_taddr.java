package com.gravilink.zcash;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.gravilink.zcash.crypto.Base58;
import com.gravilink.zcash.crypto.ECKey;
import com.gravilink.zcash.crypto.Utils;

import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import ove.crypto.digest.Blake2b;

public class ZCashTransaction_taddr {

  int version = 3;
  Vector<Tx_in> inputs = new Vector<>();
  Vector<Tx_out> outputs = new Vector<>();
  Vector<JoinSplit> joinSplits = new Vector<>();
  int locktime = 0;
  int expiryHeight = 499999999;

  ECKey privKey;

  public ZCashTransaction_taddr(ECKey privKey, String fromAddr, String toAddr, Long value, Long fee,
                                Iterable<ZCashTransactionOutput> outputs) throws IllegalArgumentException {
    this.privKey = privKey;
    byte[] fromKeyHash = Arrays.copyOfRange(Base58.decodeChecked(fromAddr), 2, 22);
    byte[] toKeyHash = Arrays.copyOfRange(Base58.decodeChecked(toAddr), 2, 22);
    long value_pool = 0;

    for (ZCashTransactionOutput out : outputs) {
      inputs.add(new Tx_in(out, fromKeyHash, out.value));
      value_pool += out.value;
    }

    this.outputs.add(new Tx_out(toKeyHash, value));
    if (value_pool - value - fee > 0) {
      this.outputs.add(new Tx_out(fromKeyHash, value_pool - value - fee));
    } else if (value_pool - value - fee < 0) {
      throw new IllegalArgumentException("Found UTXOs cannot fund this transaction.");
    }
  }

  public byte[] getBytes() throws ZCashException {
    // fOverwintered and nVersion
    byte[] header = new byte[4];
    header[0] = (byte) (0x03);
    header[1] = (byte) (0x00);
    header[2] = (byte) (0x00);
    header[3] = (byte) (0x80);

    // versionGroupId
    byte[] versionGroupId = BaseEncoding.base16().decode("03C48270");
    List<Byte> vgid = Bytes.asList(versionGroupId);
    Collections.reverse(vgid);
    versionGroupId = Bytes.toArray(vgid);
    byte[] tx_bytes = Bytes.concat(header, versionGroupId);

    tx_bytes = Bytes.concat(tx_bytes, Utils.compactSizeIntLE(inputs.size()));
    for (int i = 0; i < inputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, inputs.get(i).getBytes(i));
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.compactSizeIntLE(outputs.size()));
    for (int i = 0; i < outputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, outputs.get(i).getBytes());
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.int32BytesLE(locktime));
    tx_bytes = Bytes.concat(tx_bytes, Utils.int32BytesLE(expiryHeight));
    //number JoinSplits
    tx_bytes = Bytes.concat(tx_bytes, Utils.compactSizeIntLE(joinSplits.size()));


    return tx_bytes;
  }

  private byte[] getInputSignature(int index) throws ZCashException {
    //1. header
    byte[] header = new byte[4];
    header[0] = (byte) (0x03);
    header[1] = (byte) (0x00);
    header[2] = (byte) (0x00);
    header[3] = (byte) (0x80);

    //2. versionGroupId
    byte[] versionGroupId = BaseEncoding.base16().decode("03C48270");
    List<Byte> vgid = Bytes.asList(versionGroupId);
    Collections.reverse(vgid);
    versionGroupId = Bytes.toArray(vgid);

    byte[] hashPrevouts = new byte[32];
    hashPrevouts = getPrevoutHash();
    byte[] hashSequence = new byte[32];
    hashSequence = getSequenceHash();
    byte[] hashOutputs = new byte[32];
    hashOutputs = getOutputsHash();
    byte[] hashJoinSplits = new byte[32];
    byte[] lt = Utils.int32BytesLE(locktime);
    byte[] eh = Utils.int32BytesLE(expiryHeight);
    byte[] sh = Utils.int32BytesLE(0x01);

    // https://github.com/zcash/zips/blob/master/zip-0143.rst#specification
    // p. 10 (a, b, c, d)
    byte[] hash = inputs.get(index).txid;
    byte[] in = Utils.int32BytesLE(inputs.get(index).index);
    byte[] scr = inputs.get(index).script;
    byte[] v = Utils.int64BytesLE(inputs.get(index).value);
    byte[] sq = Utils.int32BytesLE(inputs.get(index).sequence);

    //build tx
    byte[] tx_bytes = Bytes.concat(header,
            versionGroupId,
            hashPrevouts,
            hashSequence,
            hashOutputs,
            hashJoinSplits,
            lt,
            eh,
            sh,
            hash,
            in,
            scr,
            v,
            sq);

    byte[] persn = Bytes.concat("ZcashSigHash".getBytes(), Utils.int32BytesLE(0x5ba81b19));
    byte[] txBlakeHash = getBlake2bHash(tx_bytes, persn);

    ECKey.ECDSASignature sig = privKey.signZcash(txBlakeHash);
    sig = sig.toCanonicalised();
    ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
    try {
      DERSequenceGenerator seq = new DERSequenceGenerator(bos);
      seq.addObject(new ASN1Integer(sig.r));
      seq.addObject(new ASN1Integer(sig.s));
      seq.close();
    } catch (IOException e) {
      throw new ZCashException("Cannot encode signature into transaction in ZCashTransaction_taddr.getInputSignature", e);
    }

    return bos.toByteArray();
  }

  private byte[] getPrevoutHash() {
    byte[] prevouts = new byte[32];
    for (int i = 0; i < inputs.size(); i++) {
      prevouts = Bytes.concat(prevouts, inputs.get(i).txid, Utils.int32BytesLE(inputs.get(i).index));
    }
    return getBlake2bHash(prevouts, "ZcashPrevoutHash".getBytes());
  }

  private byte[] getSequenceHash() {
    byte[] seq = new byte[32];
    for (int i = 0; i < inputs.size(); i++) {
      seq = Bytes.concat(seq, Utils.int32BytesLE(inputs.get(i).sequence));
    }
    return getBlake2bHash(seq, "ZcashSequencHash".getBytes());
  }

  private byte[] getOutputsHash() {
    byte[] outs = new byte[32];
    for (int i = 0; i < inputs.size(); i++) {
      outs = Bytes.concat(outs, outputs.get(i).getBytes());
    }
    return getBlake2bHash(outs, "ZcashOutputsHash".getBytes());
  }

  private byte[] getBlake2bHash(byte[] bytes, byte[] persn) {
    Blake2b.Param param = new Blake2b.Param().
            setDigestLength(32).
            setPersonal (persn);
    final Blake2b blake2b = Blake2b.Digest.newInstance(param);
    blake2b.update(bytes);
    return blake2b.digest();
  }

  private class Tx_in {
    byte[] txid;
    long index;
    byte[] script;
    int sequence = 0xffffffff;
    Long value;

    Tx_in(ZCashTransactionOutput base, byte[] pubKeyHash, Long value) {
      List<Byte> txbytes = Bytes.asList(Utils.hexToBytes(base.txid));
      Collections.reverse(txbytes);
      txid = Bytes.toArray(txbytes);//May be incorrect
      index = base.n;
      script = Utils.hexToBytes(base.hex);
      this.value = value;
    }

    byte[] getBytes(boolean isEmpty) {
      byte[] result = Bytes.concat(txid, Utils.int32BytesLE(index));

      if (isEmpty) {
        result = Bytes.concat(result, new byte[]{0x0});
      } else {
        result = Bytes.concat(result, Utils.compactSizeIntLE(script.length), script);
      }

      return Bytes.concat(result, Utils.int32BytesLE(sequence));
    }

    byte[] getBytes(int index) throws ZCashException {
      byte[] sign = Bytes.concat(getInputSignature(index), new byte[]{1});
      byte[] pubKey = privKey.getPubKeyPoint().getEncoded(true);
      return Bytes.concat(txid,
              Utils.int32BytesLE(this.index),
              Utils.compactSizeIntLE(sign.length + pubKey.length +
                      Utils.compactSizeIntLE(sign.length).length +
                      Utils.compactSizeIntLE(pubKey.length).length),
              Utils.compactSizeIntLE(sign.length),
              sign,
              Utils.compactSizeIntLE(pubKey.length),
              pubKey,
              Utils.int32BytesLE(sequence));
    }
  }

  private class Tx_out {
    long value;
    byte[] script;

    Tx_out(byte[] pubKeyHash, long value) {
      this.value = value;
      script = Bytes.concat(new byte[]{(byte) 0x76, (byte) 0xa9, (byte) 0x14}, pubKeyHash, new byte[]{(byte) 0x88, (byte) 0xac});
    }

    byte[] getBytes() {
      return Bytes.concat(Utils.int64BytesLE(value), Utils.compactSizeIntLE(script.length), script);
    }
  }

  private class JoinSplit {
    JoinSplit() {
    }

    byte[] getBytes() {
      return new byte[0];
    }
  }

}
