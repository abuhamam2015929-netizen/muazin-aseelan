package com.aseelan.adhan.data

/**
 * قائمة المؤذنين المتاحين (6 أصوات).
 * سيتم لاحقاً وضع الملفات الصوتية الفعلية في res/raw باسم rawResName
 * (مثال: adhan_zailai.mp3 -> R.raw.adhan_zailai)
 */
data class Muadhin(
    val id: Int,
    val name: String,
    val rawResName: String
)

object MuadhinList {
    val all = listOf(
        Muadhin(1, "الشيخ عبدالله الزيلعي", "adhan_zailai"),
        Muadhin(2, "الحرم المكي - محمود فضل", "adhan_haram_mahmoud_fadel"),
        Muadhin(3, "الشيخ ناصر القطامي", "adhan_qatami"),
        Muadhin(4, "المؤذن محمد جازي عبدالله", "adhan_jazi"),
        Muadhin(5, "الحرم المكي - سعيد بن عمر فلاته", "adhan_haram_saeed_falatah"),
        Muadhin(6, "المؤذن محمد مروان قصاص", "adhan_qassas")
    )

    val default = all.first()

    fun byId(id: Int): Muadhin = all.firstOrNull { it.id == id } ?: default
}
