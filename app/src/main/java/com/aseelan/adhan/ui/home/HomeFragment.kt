private fun renderDates() {
        val cal = Calendar.getInstance()

        val dayNameFormat = SimpleDateFormat("EEEE", Locale("ar"))
        binding.textDayName.text = dayNameFormat.format(cal.time)

        val gregorianFormat = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        binding.textGregorianDate.text = gregorianFormat.format(cal.time)

        val hijri = HijriDateConverter.fromGregorian(cal, settings.getHijriDayOffset())
        binding.textHijriDate.text = hijri.formatted()
    }
