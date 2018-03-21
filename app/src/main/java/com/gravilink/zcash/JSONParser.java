package com.gravilink.zcash;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class JSONParser {

  public static final String HASH = "hash";
  public static final String MAINCHAIN = "mainChain";
  public static final String FEE = "fee";
  public static final String TYPE = "type";
  public static final String SHIELDED = "shielded";
  public static final String INDEX = "index";
  public static final String BLOCKHASH = "blockHash";
  public static final String BLOCKHEIGHT = "blockHeight";
  public static final String VERSION = "version";
  public static final String TIMESTAMP = "timestamp";
  public static final String TIME = "time";
  public static final String VIN = "vin";
  public static final String VOUT = "vout";
  public static final String VJOINSPLIT = "vjoinsplit";
  public static final String LOCKTIME = "lockTime";
  public static final String VALUE = "value";
  public static final String OUTPUTVALUE = "outputValue";
  public static final String SHIELDEDVALUE = "shieldedValue";
  public static final String RETRIEVEDVOUT = "retrievedVout";
  public static final String N = "n";
  public static final String SCRIPTPUBKEY = "scriptPubKey";
  public static final String ADDRESSES = "addresses";
  public static final String ASM = "asm";
  public static final String HEX = "hex";
  public static final String REQSIGS = "reqSigs";
  public static final String VALUEZAT = "valueZat";
  public static final String SCRIPTSIG = "scriptSig";
  public static final String TXID = "txid";
  public static final String COINBASE = "coinbase";
  public static final String SEQUENCE = "sequence";


  public static List<ZCashTransactionDetails_taddr> parseTxArray(InputStream is) throws IOException {
    //java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    //String res = s.hasNext() ? s.next() : "";
    //InputStream stream = new ByteArrayInputStream(res.getBytes("UTF-8"));
    JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
    List<ZCashTransactionDetails_taddr> txs = null;
    try {
      txs = readTxArray(reader);
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
      Log.e("Error message", e.getMessage());
    }

    return txs;
  }

  private static List<ZCashTransactionDetails_taddr> readTxArray(JsonReader reader) throws IOException {
    List<ZCashTransactionDetails_taddr> txs = new LinkedList<>();
    reader.beginArray();
    while (reader.hasNext()) {
      txs.add(readTx(reader));
    }

    reader.endArray();
    return txs;
  }

  private static ZCashTransactionDetails_taddr readTx(JsonReader reader) throws IOException, IllegalStateException {
    ZCashTransactionDetails_taddr tx = new ZCashTransactionDetails_taddr();
    reader.beginObject();
    while (reader.peek() != JsonToken.END_OBJECT) {
      String name = reader.nextName();
      switch (name) {
        case HASH:
          tx.hash = reader.nextString();
          break;
        case MAINCHAIN:
          tx.mainChain = reader.nextBoolean();
          break;
        case FEE:
          tx.fee = Double.valueOf(reader.nextDouble() * 1e8).longValue();
          break;
        case TYPE:
          tx.type = reader.nextString();
          break;
        case SHIELDED:
          tx.shielded = reader.nextBoolean();
          break;
        case INDEX:
          tx.index = reader.nextLong();
          break;
        case BLOCKHASH:
          tx.blockHash = reader.nextString();
          break;
        case BLOCKHEIGHT:
          tx.blockHeight = reader.nextLong();
          break;
        case VERSION:
          tx.version = reader.nextLong();
          break;
        case LOCKTIME:
          tx.locktime = reader.nextLong();
          break;
        case TIME:
          tx.time = reader.nextLong();
          break;
        case TIMESTAMP:
          tx.timestamp = reader.nextLong();
          break;
        case VIN:
          tx.vin = readTxInputs(reader);
          break;
        case VOUT:
          tx.vout = readTxOutputs(reader, null);
          break;
        case VJOINSPLIT:
          skipJoinSplits(reader);
          break;
        case VALUE:
          tx.value = Double.valueOf(reader.nextDouble() * 1e8).longValue();
          break;
        case OUTPUTVALUE:
          tx.outputValue = Double.valueOf(reader.nextDouble() * 1e8).longValue();
          break;
        case SHIELDEDVALUE:
          tx.shieldedValue = Double.valueOf(reader.nextDouble() * 1e8).longValue();
          break;
        default:
          Log.i("JSON", String.format("Unexpected field: %s", name));
      }
    }
    /*
    tx.hash        = readFieldString(reader);
    tx.mainChain   = readFieldBoolean(reader);
    tx.fee         = readFieldLong(reader, true);
    tx.type        = readFieldString(reader);
    tx.shielded    = readFieldBoolean(reader);
    tx.index       = readFieldLong(reader, false);
    tx.blockHash   = readFieldString(reader);
    tx.blockHeight = readFieldLong(reader, false);
    tx.version     = readFieldLong(reader, false);
    tx.locktime    = readFieldLong(reader, false);
    tx.timestamp   = readFieldLong(reader, false);
    tx.time        = readFieldLong(reader, false);
    tx.vin         = readTxInputs(reader);
    tx.vout        = readTxOutputs(reader, tx.hash);

    //vjoinsplit
    reader.nextName();
    reader.skipValue();

    tx.value         = readFieldLong(reader, true);
    tx.outputValue   = readFieldLong(reader, true);
    tx.shieldedValue = readFieldLong(reader, true);
    */
    reader.endObject();
    return tx;
  }

  private static void skipJoinSplits(JsonReader reader) throws IOException {
    reader.skipValue();
  }

  private static Vector<ZCashTransactionOutput> readTxOutputs(JsonReader reader, String txid) throws IOException {
    Vector<ZCashTransactionOutput> vout = new Vector<>();
    //reader.nextName();
    reader.beginArray();
    while (reader.hasNext()) {
      ZCashTransactionOutput out = readTxSingleOutput(reader);
      out.txid = txid;
      vout.add(out);
    }

    reader.endArray();
    return vout;
  }

  private static ZCashTransactionOutput readTxSingleOutput(JsonReader reader) throws IOException {
    ZCashTransactionOutput output = new ZCashTransactionOutput();
    reader.beginObject(); //output
    while (reader.peek() != JsonToken.END_OBJECT) {
      String name = reader.nextName();
      switch (name) {
        case N:
          output.n = reader.nextLong();
          break;
        case SCRIPTPUBKEY:
          reader.beginObject();
          while (reader.peek() != JsonToken.END_OBJECT) {
            name = reader.nextName();
            switch (name) {
              case ADDRESSES:
                reader.beginArray();
                while (reader.hasNext()) {
                  output.address = reader.nextString();
                }
                reader.endArray();
                break;
              case ASM:
                output.asm = reader.nextString();
                break;
              case HEX:
                output.hex = reader.nextString();
                break;
              case REQSIGS:
                output.regSigs = reader.nextLong();
                break;
              case TYPE:
                output.type = reader.nextString();
                break;
              default:
                Log.i("JSON", String.format("Unexpected field: %s", name));
            }
          }
          reader.endObject();
          break;
        case VALUE:
          output.value = Double.valueOf(reader.nextDouble() * 1e8).longValue();
          break;
        case VALUEZAT:
          output.value = reader.nextLong();
          break;
        default:
          Log.i("JSON", String.format("Unexpected field: %s", name));
      }
    }
    /*
    output.n = readFieldLong(reader, false);

    reader.nextName();
    reader.beginObject(); //scriptpubkey

    reader.nextName();
    reader.beginArray(); //addresses

    output.address = reader.nextString();
    while(reader.hasNext()) {
      reader.skipValue();
    }

    reader.endArray(); //addresses end

    output.asm     = readFieldString(reader);
    output.hex     = readFieldString(reader);
    output.regSigs = readFieldLong(reader, false);
    output.type    = readFieldString(reader);

    reader.endObject(); //scriptpubkey end

    //Double value, skipped because next field has type Long and same meaning
    reader.nextName();
    reader.skipValue();

    output.value = readFieldLong(reader, false);
*/
    reader.endObject(); //output end
    return output;
  }

  private static Vector<ZCashTransactionOutput> readTxInputs(JsonReader reader) throws IOException {
    Vector<ZCashTransactionOutput> vin = new Vector<>();
    //reader.nextName();
    reader.beginArray();
    while (reader.hasNext()) {
      vin.add(readTxSingleInput(reader));
    }

    reader.endArray();
    return vin;
  }

  private static ZCashTransactionInput readTxSingleInput(JsonReader reader) throws IOException {
    ZCashTransactionInput input = new ZCashTransactionInput();
    reader.beginObject();
    while (reader.peek() != JsonToken.END_OBJECT) {
      String name = reader.nextName();
      switch (name) {
        case COINBASE:
          input.coinbase = reader.nextString();
          break;
        case SEQUENCE:
          input.sequence = reader.nextLong();
          break;
        case TXID:
          input.txid = reader.nextString();
          break;
        case VOUT:
          input.n = reader.nextLong();
          break;
        case SCRIPTSIG:
          reader.skipValue();
          break;
        case RETRIEVEDVOUT:
          input.copyDataFrom(readTxSingleOutput(reader));
          break;
        default:
          Log.i("JSON", String.format("Unexpected field: %s", name));
      }
    }
    /*
    if(name.equals("coinbase")) {
      input.coinbase = reader.nextString();
      input.sequence = readFieldLong(reader, false);
    } else {
      //retrievedVout
      input.copyDataFrom(readTxSingleOutput(reader));

      //signatures
      reader.nextName();
      reader.skipValue();

      input.sequence = readFieldLong(reader, false);
      input.txid = readFieldString(reader);

      //vout - already read in subobject retrievedVout
      reader.nextName();
      reader.skipValue();
    }
    */
    reader.endObject();
    return input;
  }

  private static long readFieldLong(JsonReader reader, boolean fromDouble) throws IOException {
    reader.nextName();
    if (fromDouble) {
      return Double.valueOf(reader.nextDouble() * 1e8).longValue();
    }

    return reader.nextLong();
  }

  private static String readFieldString(JsonReader reader) throws IOException {
    reader.nextName();
    return reader.nextString();
  }

  private static boolean readFieldBoolean(JsonReader reader) throws IOException {
    reader.nextName();
    return reader.nextBoolean();
  }
}
