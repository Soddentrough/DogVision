package com.example.dogvision.model

import androidx.compose.ui.graphics.Color
import com.example.dogvision.R

object AnimalRegistry {

    val DOG = AnimalVisionProfile(
        id = "dog",
        nameRes = R.string.animal_dog_name,
        subtitleRes = R.string.animal_dog_subtitle,
        descriptionRes = R.string.animal_dog_desc,
        scientificName = "Canis lupus familiaris",
        visualAcuity = "20/75",
        fieldOfViewDeg = 240,
        pupilType = PupilType.CIRCULAR,
        hasTapetum = true,
        tapetumBoostMultiplier = "4-5x",
        hasUvVision = false,
        cones = listOf(
            ConeData(435f, 20f, Color(0xFF2196F3), "S (435nm Blue)"),
            ConeData(555f, 35f, Color(0xFFFFEB3B), "M/L (555nm Yellow)")
        ),
        spectrumBarColors = listOf(Color(0xFF1E88E5), Color(0xFFFFD54F)),
        shaderTypeIndex = 0,
        tapetumFactor = 2.0f,
        uvSensitivityGain = 0.0f,
        desaturationFactor = 0.60f,
        primaryColor = Color(0xFFFFD700),
        iconRes = R.drawable.ic_dog_cute,
        keyFacts = listOf(R.string.dog_fact_1, R.string.dog_fact_2, R.string.dog_fact_3)
    )

    val DEER = AnimalVisionProfile(
        id = "deer",
        nameRes = R.string.animal_deer_name,
        subtitleRes = R.string.animal_deer_subtitle,
        descriptionRes = R.string.animal_deer_desc,
        scientificName = "Odocoileus virginianus",
        visualAcuity = "20/60",
        fieldOfViewDeg = 300,
        pupilType = PupilType.HORIZONTAL_SLIT,
        hasTapetum = true,
        tapetumBoostMultiplier = "18-20x",
        hasUvVision = true,
        cones = listOf(
            ConeData(455f, 22f, Color(0xFF00E5FF), "S (455nm UV-Open Blue)", isUv = true),
            ConeData(537f, 28f, Color(0xFF81C784), "M (537nm Green)")
        ),
        spectrumBarColors = listOf(Color(0xFF00E5FF), Color(0xFF81C784), Color(0xFF8D6E63)),
        shaderTypeIndex = 1,
        tapetumFactor = 4.5f,
        uvSensitivityGain = 1.6f,
        desaturationFactor = 0.55f,
        primaryColor = Color(0xFF00E5FF),
        iconRes = null,
        keyFacts = listOf(
            R.string.deer_fact_1,
            R.string.deer_fact_2,
            R.string.deer_fact_3,
            R.string.deer_fact_4,
            R.string.deer_fact_5
        )
    )

    val CAT = AnimalVisionProfile(
        id = "cat",
        nameRes = R.string.animal_cat_name,
        subtitleRes = R.string.animal_cat_subtitle,
        descriptionRes = R.string.animal_cat_desc,
        scientificName = "Felis catus",
        visualAcuity = "20/100",
        fieldOfViewDeg = 200,
        pupilType = PupilType.VERTICAL_SLIT,
        hasTapetum = true,
        tapetumBoostMultiplier = "6-8x",
        hasUvVision = false,
        cones = listOf(
            ConeData(450f, 20f, Color(0xFF03A9F4), "S (450nm Blue)"),
            ConeData(554f, 32f, Color(0xFFAED581), "M/L (554nm Green-Yellow)")
        ),
        spectrumBarColors = listOf(Color(0xFF0288D1), Color(0xFFAED581)),
        shaderTypeIndex = 2,
        tapetumFactor = 3.5f,
        uvSensitivityGain = 0.0f,
        desaturationFactor = 0.40f,
        primaryColor = Color(0xFF69F0AE),
        iconRes = null,
        keyFacts = listOf(R.string.cat_fact_1, R.string.cat_fact_2, R.string.cat_fact_3)
    )

    val BIRD = AnimalVisionProfile(
        id = "bird",
        nameRes = R.string.animal_bird_name,
        subtitleRes = R.string.animal_bird_subtitle,
        descriptionRes = R.string.animal_bird_desc,
        scientificName = "Buteo / Passeriformes",
        visualAcuity = "20/5",
        fieldOfViewDeg = 340,
        pupilType = PupilType.TUBULAR_DOUBLE_FOVEA,
        hasTapetum = false,
        tapetumBoostMultiplier = "1x (Diurnal)",
        hasUvVision = true,
        cones = listOf(
            ConeData(380f, 15f, Color(0xFFE040FB), "UV (380nm Ultraviolet)", isUv = true),
            ConeData(450f, 18f, Color(0xFF2979FF), "SWS (450nm Blue)"),
            ConeData(535f, 22f, Color(0xFF00E676), "MWS (535nm Green)"),
            ConeData(570f, 25f, Color(0xFFFF1744), "LWS (570nm Red)")
        ),
        spectrumBarColors = listOf(
            Color(0xFFD500F9),
            Color(0xFF2979FF),
            Color(0xFF00E676),
            Color(0xFFFFD600),
            Color(0xFFFF1744)
        ),
        shaderTypeIndex = 3,
        tapetumFactor = 0.0f,
        uvSensitivityGain = 2.0f,
        desaturationFactor = 1.0f,
        primaryColor = Color(0xFFE040FB),
        iconRes = null,
        keyFacts = listOf(R.string.bird_fact_1, R.string.bird_fact_2, R.string.bird_fact_3)
    )

    val BEE = AnimalVisionProfile(
        id = "bee",
        nameRes = R.string.animal_bee_name,
        subtitleRes = R.string.animal_bee_subtitle,
        descriptionRes = R.string.animal_bee_desc,
        scientificName = "Apis mellifera",
        visualAcuity = "Compound",
        fieldOfViewDeg = 280,
        pupilType = PupilType.COMPOUND_HEXAGONAL,
        hasTapetum = false,
        tapetumBoostMultiplier = "1x (Diurnal)",
        hasUvVision = true,
        cones = listOf(
            ConeData(345f, 18f, Color(0xFFD500F9), "UV (345nm Nectar Guide)", isUv = true),
            ConeData(440f, 20f, Color(0xFF3D5AFE), "B (440nm Blue)"),
            ConeData(540f, 25f, Color(0xFF76FF03), "G (540nm Green)")
        ),
        spectrumBarColors = listOf(Color(0xFFD500F9), Color(0xFF3D5AFE), Color(0xFF76FF03), Color(0xFF212121)),
        shaderTypeIndex = 4,
        tapetumFactor = 0.0f,
        uvSensitivityGain = 2.2f,
        desaturationFactor = 0.9f,
        primaryColor = Color(0xFFFFAB00),
        iconRes = null,
        keyFacts = listOf(R.string.bee_fact_1, R.string.bee_fact_2, R.string.bee_fact_3)
    )

    val SNAKE = AnimalVisionProfile(
        id = "snake",
        nameRes = R.string.animal_snake_name,
        subtitleRes = R.string.animal_snake_subtitle,
        descriptionRes = R.string.animal_snake_desc,
        scientificName = "Crotalinae",
        visualAcuity = "20/150+IR",
        fieldOfViewDeg = 200,
        pupilType = PupilType.VERTICAL_SLIT,
        hasTapetum = true,
        tapetumBoostMultiplier = "3x",
        hasUvVision = false,
        cones = listOf(
            ConeData(460f, 22f, Color(0xFF00B0FF), "S (460nm Blue)"),
            ConeData(555f, 30f, Color(0xFFFFC107), "L (555nm Green/Amber)"),
            ConeData(680f, 40f, Color(0xFFFF3D00), "Pit Organ (8-12µm Thermal IR)")
        ),
        spectrumBarColors = listOf(Color(0xFF00B0FF), Color(0xFFFFC107), Color(0xFFFF3D00)),
        shaderTypeIndex = 5,
        tapetumFactor = 1.5f,
        uvSensitivityGain = 0.0f,
        desaturationFactor = 0.5f,
        primaryColor = Color(0xFFFF3D00),
        iconRes = null,
        keyFacts = listOf(R.string.snake_fact_1, R.string.snake_fact_2, R.string.snake_fact_3)
    )

    val ALL_ANIMALS: List<AnimalVisionProfile> = listOf(
        DOG,
        DEER,
        CAT,
        BIRD,
        BEE,
        SNAKE
    )

    fun getById(id: String): AnimalVisionProfile {
        return ALL_ANIMALS.firstOrNull { it.id == id } ?: DOG
    }
}
