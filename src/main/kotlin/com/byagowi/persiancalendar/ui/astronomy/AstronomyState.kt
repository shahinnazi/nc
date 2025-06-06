package com.byagowi.persiancalendar.ui.astronomy

import android.content.res.Resources
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.global.coordinates
import com.byagowi.persiancalendar.global.language
import com.byagowi.persiancalendar.global.spacedColon
import com.byagowi.persiancalendar.utils.formatDateAndTime
import com.byagowi.persiancalendar.utils.generateYearName
import com.byagowi.persiancalendar.utils.sunlitSideMoonTiltAngle
import com.byagowi.persiancalendar.utils.titleStringId
import com.byagowi.persiancalendar.utils.toGregorianCalendar
import com.byagowi.persiancalendar.utils.toObserver
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.helioVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchGlobalSolarEclipse
import io.github.cosinekitty.astronomy.searchLocalSolarEclipse
import io.github.cosinekitty.astronomy.searchLunarEclipse
import io.github.cosinekitty.astronomy.sunPosition
import java.util.Date
import java.util.GregorianCalendar

class AstronomyState(val date: GregorianCalendar) {
    private val time = Time.fromMillisecondsSince1970(date.time.time)
    val sun = sunPosition(time)
    val moon = eclipticGeoMoon(time)
    private val observer by lazy(LazyThreadSafetyMode.NONE) { coordinates.value?.toObserver() }
    val moonTilt by lazy(LazyThreadSafetyMode.NONE) {
        observer?.let { observer -> sunlitSideMoonTiltAngle(time, observer).toFloat() }
    }
    val sunAltitude by lazy(LazyThreadSafetyMode.NONE) {
        val observer = observer ?: return@lazy null
        val sunEquator =
            equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        horizon(time, observer, sunEquator.ra, sunEquator.dec, Refraction.Normal).altitude
    }
    val moonAltitude by lazy(LazyThreadSafetyMode.NONE) {
        val observer = observer ?: return@lazy null
        val moonEquator =
            equator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        horizon(time, observer, moonEquator.ra, moonEquator.dec, Refraction.Normal).altitude
    }
    val planets by lazy(LazyThreadSafetyMode.NONE) {
        solarSystemPlanets.map { it.titleStringId to equatorialToEcliptic(helioVector(it, time)) }
    }

    fun generateHeader(resources: Resources, jdn: Jdn): List<String> {
        val observer = coordinates.value?.toObserver()
        return listOf(
            if (observer != null) searchLocalSolarEclipse(time, observer).run { kind to peak.time }
            else searchGlobalSolarEclipse(time).run { kind to peak },
            searchLunarEclipse(time).run { kind to peak },
        ).mapIndexed { i, (kind, peak) ->
            val formattedDate =
                Date(peak.toMillisecondsSince1970()).toGregorianCalendar().formatDateAndTime()
            val isSolar = i == 0
            val title = if (isSolar) R.string.solar_eclipse else R.string.lunar_eclipse
            (language.value.tryTranslateEclipseType(isSolar, kind)
                ?: resources.getString(title)) + spacedColon + formattedDate
        } + generateYearName(
            resources = resources,
            jdn = jdn,
            withOldEraName = language.value.isPersian,
            withEmoji = true,
            time = date
        )
    }

    companion object {
        private val solarSystemPlanets = listOf(
            Body.Mercury, Body.Venus, Body.Earth, Body.Mars, Body.Jupiter, Body.Saturn,
            Body.Uranus, Body.Neptune
        )
    }
}
