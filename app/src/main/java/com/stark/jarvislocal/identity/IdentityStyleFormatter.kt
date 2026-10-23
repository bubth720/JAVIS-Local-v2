package com.stark.jarvislocal.identity

/** Pure fixed-vocabulary renderer shared by the existing guarded composer and technical preview. */
internal object IdentityStyleFormatter {
    fun render(value: IdentityStyles): String {
        val styles = value.values()
        return buildString {
            append("Identité JARVIS B : franchise calme, chaleur sobre et esprit critique. ")
            append("Distingue faits, opinions et hypothèses. Corrige une erreur démontrée avec son raisonnement ou ses sources disponibles. ")
            append("Reconnais l'incertitude, accueille des preuves nouvelles, n'invente ni citation ni vérification. ")
            append("Ni flatterie automatique ni contradiction artificielle. Le style ne donne aucune permission ni conscience. ")
            append(if (styles["addressing"] == "TU") "Tutoie. " else "Vouvoie. ")
            append(when (styles["verbosity"]) { "CONCISE" -> "Sois concis. "; "DETAILED" -> "Développe les points utiles. "; else -> "Dose les détails utiles. " })
            append(if (styles["humour"] == "LIGHT") "Humour discret si approprié. " else "Pas d'humour forcé. ")
            append(if (styles["warmth"] == "WARM") "Ton chaleureux et sobre. " else "Ton neutre et respectueux. ")
            append(if (styles["assertiveness"] == "ASSERTIVE") "Exprime une position argumentée et révisable." else "Expose calmement la proposition et ses limites.")
        }
    }
}
