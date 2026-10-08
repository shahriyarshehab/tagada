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
    val rejectedLead: String,
    val deviceCallHistory: String,
    val deviceContacts: String,
    val missedCalls: String,
    val receivedCalls: String,
    val dialedCalls: String,
    val syncCallHistory: String,
    val importDeviceContacts: String
)

val DICT_EN = TagadaStrings(
    appName = "Tagada",
    appSubtitle = "Smart Call & Hisab Khata CRM",
    dash = "Dashboard",
    list = "Contacts & Calls",
    hisab = "Hisab Khata",
    followup = "Follow-up",
    settings = "Settings",
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
    rejectedLead = "Disqualified",
    deviceCallHistory = "Device Call History",
    deviceContacts = "Device Contacts",
    missedCalls = "Missed Calls",
    receivedCalls = "Received Calls",
    dialedCalls = "Dialed Calls",
    syncCallHistory = "Sync Recent Calls",
    importDeviceContacts = "Import Device Contacts"
)

val DICT_BN = TagadaStrings(
    appName = "তাগাদা",
    appSubtitle = "স্মার্ট কল ও হিসাব খাতা সিআরএম",
    dash = "ড্যাশবোর্ড",
    list = "যোগাযোগ ও কল",
    hisab = "হিসাব খাতা",
    followup = "ফলো-আপ",
    settings = "সেটিংস",
    allGroups = "সব গ্রুপ",
    connected = "সংযুক্ত",
    noAnswer = "রিসিভ করেনি",
    notContacted = "যোগাযোগ হয়নি",
    paidList = "পরিশোধিত তালিকা",
    unpaidList = "বকেয়া তালিকা",
    favorites = "পছন্দের",
    notes = "দ্রুত নোট",
    typeNote = "নোট লিখুন...",
    save = "সংরক্ষণ",
    search = "নাম বা ফোন নম্বর খুঁজুন...",
    addNew = "নতুন যোগাযোগ",
    allTime = "সর্বমোট",
    days = "দিন",
    finAdd = "অর্থায়ন +",
    loan = "ঋণ",
    due = "বকেয়া",
    paid = "পরিশোধিত",
    unpaid = "বকেয়া পাওনা",
    cloudSync = "গুগল ড্রাইভ ব্যাকআপ",
    lang = "ভাষা",
    darkMode = "ডার্ক মোড",
    googleLogin = "গুগল অ্যাকাউন্ট লগইন",
    googleConnected = "গুগল সংযুক্ত",
    googleLoginBtn = "গুগল দিয়ে সাইন ইন",
    vcfUploadSettings = "ভিএফসি ফাইল আপলোড",
    vcfUploadDesc = "ডিভাইস থেকে কন্টাক্ট আপলোড করুন",
    welcomeGoogle = "স্বাগতম",
    signout = "সাইন আউট",
    hotLead = "হট লিড",
    warmLead = "ওয়ার্ম লিড",
    rejectedLead = "বাতিল",
    deviceCallHistory = "ডিভাইস কল হিস্ট্রি",
    deviceContacts = "ডিভাইস কন্টাক্টস",
    missedCalls = "মিসড কল",
    receivedCalls = "রিসিভড কল",
    dialedCalls = "ডায়ালড কল",
    syncCallHistory = "কল হিস্ট্রি সিঙ্ক করুন",
    importDeviceContacts = "ডিভাইস কন্টাক্ট আনুন"
)

fun getStrings(lang: String): TagadaStrings = if (lang == "bn") DICT_BN else DICT_EN

fun moneyFmt(n: Double): String {
    val rounded = Math.round(Math.abs(n))
    return "৳ " + String.format(Locale.US, "%,d", rounded)
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
