package com.example.ui

import java.text.SimpleDateFormat
import java.util.Locale

val CONTACT_GROUPS = listOf("General", "Active", "Arrears", "Admission", "Survey", "Paid")

data class TagadaStrings(
    val appName: String,
    val appSubtitle: String,
    val dash: String,
    val list: String,
    val hisab: String,
    val followup: String,
    val settings: String,
    val allGroups: String,
    val connected: String,
    val noAnswer: String,
    val notContacted: String,
    val paidList: String,
    val unpaidList: String,
    val favorites: String,
    val notes: String,
    val typeNote: String,
    val save: String,
    val search: String,
    val addNew: String,
    val allTime: String,
    val days: String,
    val finAdd: String,
    val loan: String,
    val due: String,
    val paid: String,
    val unpaid: String,
    val cloudSync: String,
    val lang: String,
    val darkMode: String,
    val googleLogin: String,
    val googleConnected: String,
    val googleLoginBtn: String,
    val vcfUploadSettings: String,
    val vcfUploadDesc: String,
    val welcomeGoogle: String,
    val signout: String,
    val hotLead: String,
    val warmLead: String,
    val rejectedLead: String
)

val DICT_EN = TagadaStrings(
    appName = "Tagada",
    appSubtitle = "Smart Call & Hisab Khata CRM",
    dash = "Dashboard",
    list = "Contact",
    hisab = "Hisab Khata",
    followup = "Follow-up",
    settings = "Profile",
    allGroups = "All Groups",
    connected = "Connected",
    noAnswer = "No Answer",
    notContacted = "Not Contacted",
    paidList = "Paid List",
    unpaidList = "Unpaid / Due List",
    favorites = "Favorites",
    notes = "Quick Notes",
    typeNote = "Write a note (auto-saves)...",
    save = "Save",
    search = "Search name or phone number...",
    addNew = "New Contact",
    allTime = "All Time",
    days = "Days",
    finAdd = "Finance +",
    loan = "Loan",
    due = "Due",
    paid = "Paid",
    unpaid = "Unpaid Due",
    cloudSync = "Google Drive Backup",
    lang = "Language",
    darkMode = "Dark Mode",
    googleLogin = "Google Account Login",
    googleConnected = "Google Connected",
    googleLoginBtn = "Sign in with Google",
    vcfUploadSettings = "VCF Contacts Upload",
    vcfUploadDesc = "Import .vcf contact cards directly to any group",
    welcomeGoogle = "Welcome",
    signout = "Sign Out",
    hotLead = "Hot Lead",
    warmLead = "Warm Lead",
    rejectedLead = "Disqualified"
)

val DICT_BN = TagadaStrings(
    appName = "তাগাদা",
    appSubtitle = "স্মার্ট কল ও হিসাব খাতা CRM",
    dash = "ড্যাশবোর্ড",
    list = "কন্টাক্ট",
    hisab = "হিসাব খাতা",
    followup = "ফলো-আপ",
    settings = "প্রোফাইল",
    allGroups = "সব গ্রুপ",
    connected = "যোগাযোগ হয়েছে",
    noAnswer = "উত্তর নেই",
    notContacted = "কল করা হয়নি",
    paidList = "পরিশোধিত তালিকা",
    unpaidList = "বকেয়া তালিকা",
    favorites = "গুরুত্বপূর্ণ",
    notes = "দ্রুত নোট",
    typeNote = "নোট লিখুন (অটো সেভ হবে)...",
    save = "সংরক্ষণ",
    search = "নাম অথবা মোবাইল নম্বর খুঁজুন...",
    addNew = "নতুন যোগাযোগ",
    allTime = "সব সময়",
    days = "দিন",
    finAdd = "হিসাব +",
    loan = "লোন",
    due = "বকেয়া",
    paid = "পরিশোধিত",
    unpaid = "বকেয়া বাকি",
    cloudSync = "গুগল ড্রাইভ ব্যাকআপ",
    lang = "ভাষা নির্বাচন",
    darkMode = "ডার্ক মোড",
    googleLogin = "গুগল একাউন্ট লগইন",
    googleConnected = "গুগল কানেক্টেড",
    googleLoginBtn = "Google দিয়ে লগইন করুন",
    vcfUploadSettings = "VCF কন্টাক্ট আপলোড",
    vcfUploadDesc = "ফোনের .vcf ফাইল সহজে যেকোনো গ্রুপে আনুন",
    welcomeGoogle = "স্বাগতম",
    signout = "লগআউট",
    hotLead = "সম্ভাবনাময়",
    warmLead = "বিবেচনাধীন",
    rejectedLead = "বাদ দেওয়া"
)

fun getStrings(lang: String): TagadaStrings = if (lang == "bn") DICT_BN else DICT_EN

fun moneyFmt(n: Double): String {
    val rounded = Math.round(Math.abs(n))
    return "৳" + String.format(Locale.US, "%,d", rounded)
}

fun dtFmt(d: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(d) ?: return d
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        formatter.format(date)
    } catch (e: Exception) {
        d
    }
}
