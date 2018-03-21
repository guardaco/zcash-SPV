package com.gravilink.zcash;

import com.google.common.primitives.Bytes;
import com.gravilink.zcash.crypto.Base58;
import com.gravilink.zcash.crypto.ECKey;
import com.gravilink.zcash.crypto.Sha256Hash;
import com.gravilink.zcash.crypto.Utils;

import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class ZCashTransaction_taddr {

  int version = 1;
  Vector<Tx_in> inputs = new Vector<>();
  Vector<Tx_out> outputs = new Vector<>();
  int locktime = 0;

  ECKey privKey;

  public ZCashTransaction_taddr(ECKey privKey, String fromAddr, String toAddr, Long value, Long fee,
                                Iterable<ZCashTransactionOutput> outputs) throws IllegalArgumentException {
    this.privKey = privKey;
    byte[] fromKeyHash = Arrays.copyOfRange(Base58.decodeChecked(fromAddr), 2, 22);
    byte[] toKeyHash = Arrays.copyOfRange(Base58.decodeChecked(toAddr), 2, 22);
    long value_pool = 0;

    for (ZCashTransactionOutput out : outputs) {
      inputs.add(new Tx_in(out, fromKeyHash));
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
    byte[] tx_bytes = Bytes.concat(Utils.int32BytesLE(version), Utils.compactSizeIntLE(inputs.size()));
    for (int i = 0; i < inputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, inputs.get(i).getBytes(i));
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.compactSizeIntLE(outputs.size()));
    for (int i = 0; i < outputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, outputs.get(i).getBytes());
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.int32BytesLE(locktime));

    return tx_bytes;
  }

  private byte[] getInputSignature(int index) throws ZCashException {
    byte[] tx_bytes = Bytes.concat(Utils.int32BytesLE(version), Utils.compactSizeIntLE(inputs.size()));
    for (int i = 0; i < inputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, inputs.get(i).getBytes(i != index));
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.compactSizeIntLE(outputs.size()));
    for (int i = 0; i < outputs.size(); i++) {
      tx_bytes = Bytes.concat(tx_bytes, outputs.get(i).getBytes());
    }

    tx_bytes = Bytes.concat(tx_bytes, Utils.int32BytesLE(locktime), Utils.int32BytesLE(0x1));

    Sha256Hash hash = Sha256Hash.wrap(Sha256Hash.hashTwice(tx_bytes));
    ECKey.ECDSASignature sig = privKey.sign(hash);
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

  private class Tx_in {
    byte[] txid;
    long index;
    byte[] script;
    int sequence = 0xffffffff;

    Tx_in(ZCashTransactionOutput base, byte[] pubKeyHash) {
      List<Byte> txbytes = Bytes.asList(Utils.hexToBytes(base.txid));
      Collections.reverse(txbytes);
      txid = Bytes.toArray(txbytes);//May be incorrect
      index = base.n;
      script = Utils.hexToBytes(base.hex);
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

}
