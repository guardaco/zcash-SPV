# Zcash SPV library by [Guarda](https://guarda.co)

## About
Example android app. Only t-addresses now available.
Just import project in your IDE and build.


## Installation
Add this folder in your project:
```
zcash-SPV/tree/master/app/src/main/java/com/gravilink/zcash
```
and delete:
```
zcash-SPV/blob/master/app/src/main/java/com/gravilink/zcash/TestActivity.java
```
## Usage
[ZCashWalletManager.java](/app/src/main/java/com/gravilink/zcash/ZCashWalletManager.java) has folowing public methods:
- ```generateNewPrivateKey_taddr```
- ```publicKeyFromPrivateKey_taddr```
- ```getBalance_taddr```
- ```createTransaction_taddr```
- ```pushTransaction_taddr```
- ```getTransactionHistory_taddr```
- ```importWallet_taddr```
- ```getTransactionCache```

Before using ```generateNewPrivateKey_taddr``` ```BrainKeyDict``` must be initialized with ```BrainKeyDict.init()```.

## License

Library are licensed under the [MIT](/LICENSE.md) License.


Enjoy! Guarda Team hopes you will like using our lib as much as we liked creating them.
