package com.example.s1611382.ilp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson

class TestConditions {
    private var coinWallet : ArrayList<Coin> = arrayListOf()
    private var coinBank : ArrayList<Coin> = arrayListOf()
    companion object {
        private const val EMAIL_EMPTY = "empty@test.com"
        private const val EMAIL_TEST = "test@email.com"
        private const val EMAIL_COINY = "coiny@test.com"
        private const val EMAIL_POOR_CASH = "poor@test.com"
        private const val EMAIL_POOR_BANK = "poorbank@test.com"
        private const val EMAIL_TRADE_READY = "tradeready@test.com"
        private const val PASSWORD = "password"
    }

    private fun firestoreSetup(): FirebaseFirestore? {
        val firestore = FirebaseFirestore.getInstance()
        val firebaseSettings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore.firestoreSettings = firebaseSettings
        return firestore
    }

    /**
     * update Firestore with contents of coinList
     */
    private fun uploadCoins(key: String, email: String, coinList: ArrayList<Coin>) {
        val firestore = firestoreSetup()
        val gson = Gson()
        val json = gson.toJson(coinList)
        val data = HashMap<String, Any>()
        data[key] = json
        firestore?.collection(BaseActivity.COLLECTION_KEY)
                ?.document(email)
                ?.set(data, SetOptions.merge())
    }

    fun signOutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    /**
     * returns email of a user that doesn't have a firestore document
     */
    fun emptyUser(): String {
        val firestore = firestoreSetup()
        firestore?.collection("Users")?.document(EMAIL_EMPTY)?.delete()
        return EMAIL_EMPTY
    }

    /**
     * sets up and returns email of a user that owns 16 coins, the last one being 3.33 PENY
     * doesn't own any gold though
     */
    fun coinyUser(): String {
        val firestore = firestoreSetup()
        firestore?.collection("Users")?.document(EMAIL_COINY)?.delete()
        val testQuid = Coin(id = "test", value = 5.toFloat(), currency = "QUID")
        val testPeny = Coin(id = "test", value = 3.33.toFloat(), currency = "PENY")
        for (i in 1..15) {
            coinWallet.add(testQuid)
        }
        coinWallet.add(testPeny)
        uploadCoins(BaseActivity.WALLET_KEY, EMAIL_COINY, coinWallet)
        return EMAIL_COINY
    }

    /**
     * sets up and returns email of a user that owns only 1 dollar cash and no gold :(
     */
    fun poorCashUser(): String {
        val firestore = firestoreSetup()
        firestore?.collection("Users")?.document(EMAIL_POOR_CASH)?.delete()
        val testDolr = Coin(id = "test", value = 1.toFloat(), currency = "DOLR")
        coinWallet.add(testDolr)
        uploadCoins(BaseActivity.WALLET_KEY, EMAIL_POOR_CASH, coinWallet)
        return EMAIL_POOR_CASH
    }

    /**
     * sets up and returns email of a user that owns only 1 dollar in bank and no gold :(
     */
    fun poorBankUser(): String {
        val firestore = firestoreSetup()
        firestore?.collection("Users")?.document(EMAIL_POOR_CASH)?.delete()
        val testDolr = Coin(id = "test", value = 1.toFloat(), currency = "DOLR")
        coinBank.add(testDolr)
        uploadCoins(BaseActivity.BANK_KEY, EMAIL_POOR_BANK, coinBank)
        return EMAIL_POOR_BANK
    }

    /**
     * sets up and returns email of a user that is poor but able to send coins.
     */
    fun tradeReadyUser(): String {
        val firestore = firestoreSetup()
        firestore?.collection("Users")?.document(EMAIL_TRADE_READY)?.delete()
        val testDolr = Coin(id = "test", value = 1.toFloat(), currency = "DOLR")
        coinWallet.add(testDolr)
        uploadCoins(BaseActivity.WALLET_KEY, EMAIL_TRADE_READY, coinWallet)
        val document = firestore?.collection(BaseActivity.COLLECTION_KEY)?.document(EMAIL_TRADE_READY)
        val data = HashMap<String, Any>()
        // set depositCounter to 25, so user only has spare change. This means they can send coins.
        data[BaseActivity.COUNTER_KEY] = 25
        document?.set(data, SetOptions.merge())
        return EMAIL_TRADE_READY
    }


    /**
     * returns email of test user with unknown number of coins, GOLD, etc.
     * this means behaviour can change between test runs, so use with caution.
     * Might be useful to find bugs as configuration might be different from any of the other ones
     */
    fun testUser(): String {
        return EMAIL_TEST
    }

    fun getPassword(): String {
        return PASSWORD
    }

}
