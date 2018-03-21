package com.gravilink.zcash;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.gravilink.zcash.crypto.BrainKeyDict;
import com.gravilink.zcash.crypto.Utils;
import com.gravilink.zcash.ZCashWalletManager.UpdateRequirement;


import java.io.IOException;
import java.util.List;
import java.util.Vector;


public class TestActivity extends AppCompatActivity {

  ZCashWalletManager zcash;
  String privateKey;
  String publicKey;
  ZCashTransaction_taddr lastTx;
  String lastTxhex;
  UpdateRequirement requirement;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    requirement = UpdateRequirement.NO_UPDATE;
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test);
    final TestActivity self = this;

    try {
      BrainKeyDict.init(this.getAssets());
    } catch (IOException e) {
      e.printStackTrace();
    }


    String generatedPrivateKey;
    String generatedPublicKey;
    try {
      generatedPrivateKey = ZCashWalletManager.generateNewPrivateKey_taddr();
      generatedPublicKey = ZCashWalletManager.publicKeyFromPrivateKey_taddr(generatedPrivateKey);
      Log.i("PRIVATE", generatedPrivateKey);
      Log.i("PUBLIC ", generatedPublicKey);
    } catch (ZCashException e) {
      e.printStackTrace();
    }
    privateKey = "KwxzbSmXp1mDHW2aBrXaYMbPWveKT8XWNjWdnVgPXjh4eP4WP9Xk";
    try {
      publicKey = ZCashWalletManager.publicKeyFromPrivateKey_taddr(privateKey);//"t1V4EGWyNiQBX4k9jtzBvFUzmDArCnGa8eH";
    } catch (ZCashException e) {
      Log.e("ONCREATE", "CANNOT CREATE PUBLIC KEY");
    }
    try {
      zcash = new ZCashWalletManager("https://zec.guarda.co:443", "guarda", "X5at2B-rGKmVDVMeqEnv4-l9w67cDsXww4pmudHq05g=");
    } catch (ZCashException e) {
      e.printStackTrace();
    }

    findViewById(R.id.wallet).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          zcash.importWallet_taddr(privateKey,
                  requirement,
                  new WalletCallback<String, Void>() {
                    @Override
                    public void onResponse(String r1, Void r2) {
                      Log.i("RESPONSE CODE", r1);
                    }
                  });
        } catch (ZCashException e) {
          e.printStackTrace();
        }

      }
    });

    findViewById(R.id.balance).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          zcash.getBalance_taddr(publicKey,
                  new WalletCallback<String, Long>() {
                    @Override
                    public void onResponse(String r1, Long r2) {
                      Log.i("RESPONSE CODE", r1);
                      if (r1.equals("ok")) {
                        Log.i("RESPONSE VALUE", r2.toString());
                      }
                    }
                  });
        } catch (ZCashException e) {
          e.printStackTrace();
        }
      }
    });

    findViewById(R.id.history).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          zcash.getTransactionHistory_taddr(publicKey, 10, 1, requirement,
                  new WalletCallback<String, List<ZCashTransactionDetails_taddr>>() {
                    @Override
                    public void onResponse(String r1, List<ZCashTransactionDetails_taddr> r2) {
                      Log.i("RESPONSE CODE", r1);
                      if (r1.equals("ok")) {
                        Log.i("RESPONSE VALUE", String.format("%d transactions returned.", r2.size()));
                      }
                    }
                  });
        } catch (ZCashException e) {
          e.printStackTrace();
        }
      }
    });

    findViewById(R.id.transfer).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          zcash.pushTransaction_taddr(lastTx, new WalletCallback<String, Void>() {
            @Override
            public void onResponse(String r1, Void r2) {
              Log.i("RESPONSE CODE", r1);
            }
          });
        } catch (ZCashException e) {
          e.printStackTrace();
        }
      }
    });

    findViewById(R.id.create).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          zcash.createTransaction_taddr(publicKey,
                  publicKey, 10061L, 100000L, privateKey,
                  0,
                  new WalletCallback<String, ZCashTransaction_taddr>() {
                    @Override
                    public void onResponse(String r1, ZCashTransaction_taddr r2) {
                      Log.i("RESPONSE CODE", r1);
                      if (r1.equals("ok")) {
                        self.lastTx = r2;
                        try {
                          self.lastTxhex = Utils.bytesToHex(r2.getBytes());
                        } catch (ZCashException e) {
                          Log.i("TX", "Cannot sign transaction");
                        }
                      }
                    }
                  });
        } catch (ZCashException e) {
          e.printStackTrace();
        }
      }
    });

    findViewById(R.id.debug).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (lastTxhex != null) {
          Log.i("TX", lastTxhex);
        }
        switch (requirement) {
          case NO_UPDATE:
            requirement = UpdateRequirement.TRY_UPDATE;
            break;
          case TRY_UPDATE:
            requirement = UpdateRequirement.REQUIRE_UPDATE;
            break;
          case REQUIRE_UPDATE:
            requirement = UpdateRequirement.NO_UPDATE;
            break;
        }
        Log.i("REQUIREMENT", requirement.toString());
        boolean sorted = true;
        Vector<ZCashTransactionDetails_taddr> cache = zcash.getTransactionCache();
        if (cache != null && cache.size() > 1) {
          ZCashTransactionDetails_taddr first = cache.get(0);
          for (int i = 1; i < cache.size(); i++) {
            ZCashTransactionDetails_taddr second = cache.get(i);
            sorted &= first.blockHeight <= second.blockHeight;
            first = second;
          }
        }
      }
    });
  }
}
