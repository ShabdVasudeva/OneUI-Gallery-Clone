package apw.sec.android.gallery;

object SubtitleBridge {
    private val subtitle: MutableMap<String, String> = mutableMapOf();

    fun save(key: String, subtitle: String){
        this.subtitle[key] = subtitle
    }

    fun remove(key: String){
        this.subtitle.remove(key)
    }

    fun get(key: String): String?{
        return this.subtitle[key]
    }
}