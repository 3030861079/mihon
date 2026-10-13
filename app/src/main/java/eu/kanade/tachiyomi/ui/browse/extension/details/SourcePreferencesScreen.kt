package eu.kanade.tachiyomi.ui.browse.extension.details

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.R
import androidx.preference.forEach
import androidx.preference.getOnBindEditTextListener
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.sourcePreferences
import eu.kanade.tachiyomi.widget.TachiyomiTextInputEditText.Companion.setIncognito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen
import android.os.SystemClock
import java.util.concurrent.TimeUnit

class SourcePreferencesScreen(val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val source by produceState<Source?>(initialValue = null) {
            value = context.appGraph.sourceManager.getOrStub(sourceId)
        }

        if (source == null) {
            LoadingScreen()
            return
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = source.toString(),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            FragmentContainer(
                fragmentManager = (context as FragmentActivity).supportFragmentManager,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                add(it, SourcePreferencesFragment.getInstance(sourceId), null)
            }
        }
    }

    /**
     * From https://stackoverflow.com/questions/60520145/fragment-container-in-jetpack-compose/70817794#70817794
     */
    @Composable
    private fun FragmentContainer(
        fragmentManager: FragmentManager,
        modifier: Modifier = Modifier,
        commit: FragmentTransaction.(containerId: Int) -> Unit,
    ) {
        val containerId by rememberSaveable {
            mutableIntStateOf(View.generateViewId())
        }
        var initialized by rememberSaveable { mutableStateOf(false) }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                FragmentContainerView(context)
                    .apply { id = containerId }
            },
            update = { view ->
                if (!initialized) {
                    fragmentManager.commit { commit(view.id) }
                    initialized = true
                } else {
                    fragmentManager.onContainerAvailable(view)
                }
            },
        )
    }

    /** Access to package-private method in FragmentManager through reflection */
    private fun FragmentManager.onContainerAvailable(view: FragmentContainerView) {
        val method = FragmentManager::class.java.getDeclaredMethod(
            "onContainerAvailable",
            FragmentContainerView::class.java,
        )
        method.isAccessible = true
        method.invoke(this, view)
    }
}

class SourcePreferencesFragment : PreferenceFragmentCompat() {

    override fun getContext(): Context? {
        val superCtx = super.getContext() ?: return null
        val tv = TypedValue()
        superCtx.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(superCtx, tv.resourceId)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        lifecycleScope.launch {
            preferenceScreen = populateScreen()
        }
    }

    private suspend fun populateScreen(): PreferenceScreen {
        val sourceId = requireArguments().getLong(SOURCE_ID)
        val appGraph = requireContext().appGraph
        val source = appGraph.sourceManager.getOrStub(sourceId)
        val basePreferences = appGraph.basePreferences
        val sourceScreen = preferenceManager.createPreferenceScreen(requireContext())

        if (source is ConfigurableSource) {
            val sourcePreferences = withContext(Dispatchers.IO) { source.sourcePreferences().also { it.all } }
            val dataStore = SharedPreferencesDataStore(sourcePreferences)
            preferenceManager.preferenceDataStore = dataStore

            source.setupPreferenceScreen(sourceScreen)
            sourceScreen.forEach { pref ->
                pref.isIconSpaceReserved = false
                pref.isSingleLineTitle = false
                if (pref is DialogPreference && pref.dialogTitle.isNullOrEmpty()) {
                    pref.dialogTitle = pref.title
                }

                // Apply incognito IME for EditTextPreference
                if (pref is EditTextPreference) {
                    val setListener = pref.getOnBindEditTextListener()
                    pref.setOnBindEditTextListener {
                        setListener?.onBindEditText(it)
                        it.setIncognito(basePreferences, lifecycleScope)
                    }
                }
            }

            insertMirrorTestPreferences(sourceScreen)
        }

        return sourceScreen
    }

    // -----------------------------------------------------------------------------------------
    // Mirror speed test
    // -----------------------------------------------------------------------------------------

    private var mirrorTestJob: Job? = null

    /** Bare client on purpose: the test should reflect raw mirror reachability, not app plumbing. */
    private val mirrorTestClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Finds mirror pickers on the screen and inserts a speed-test entry right below each one.
     *
     * Mirror URLs live inside the extension, so the host can only infer them: either the picker's
     * entry values are URLs directly, or (keiyoushi MirrorPreferences style) its values are
     * consecutive indexes and the labels are bare hostnames. Anything else is skipped silently.
     */
    private fun insertMirrorTestPreferences(screen: PreferenceScreen) {
        val tests = mutableListOf<Pair<Preference, Preference>>()
        screen.forEach { pref ->
            if (pref !is ListPreference) return@forEach
            val urls = mirrorCandidates(pref) ?: return@forEach
            val labels = pref.entries?.map { it.toString() }.orEmpty()
            tests += pref to buildMirrorTestPreference(pref, urls, labels)
        }
        if (tests.isEmpty()) return

        val reordered = mutableListOf<Preference>()
        screen.forEach { pref ->
            reordered += pref
            tests.firstOrNull { it.first === pref }?.let { reordered += it.second }
        }
        screen.removeAll()
        reordered.forEach(screen::addPreference)
    }

    private fun mirrorCandidates(pref: ListPreference): List<String>? {
        val values = pref.entryValues?.map { it.toString() } ?: return null
        val labels = pref.entries?.map { it.toString() } ?: return null
        if (values.size < 2 || values.size != labels.size) return null
        return when {
            values.all { it.startsWith("http") } -> values
            values.indices.all { values[it] == it.toString() } &&
                labels.all { it.matches(MIRROR_DOMAIN_REGEX) } -> labels.map { "https://$it" }
            else -> null
        }
    }

    private fun buildMirrorTestPreference(
        mirrorPref: ListPreference,
        urls: List<String>,
        labels: List<String>,
    ): Preference {
        val context = requireContext()
        return Preference(context).apply {
            key = "mirror_test_${mirrorPref.key}"
            title = context.stringResource(MR.strings.mirror_test_title)
            summary = context.stringResource(MR.strings.mirror_test_idle)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                runMirrorTest(mirrorPref, urls, labels, this)
                true
            }
        }
    }

    private fun runMirrorTest(
        mirrorPref: ListPreference,
        urls: List<String>,
        labels: List<String>,
        testPref: Preference,
    ) {
        val context = requireContext()
        val currentHost = when {
            urls.firstOrNull()?.startsWith("http") == true -> mirrorPref.value
            else -> mirrorPref.value.toIntOrNull()?.let { labels.getOrNull(it) }
        }?.toHostOrNull()

        mirrorTestJob?.cancel()
        mirrorTestJob = lifecycleScope.launch {
            testPref.summary = context.stringResource(MR.strings.mirror_test_running)
            val probes = urls.map { url -> async(Dispatchers.IO) { probeMirror(url) } }.awaitAll()
            val ranked = probes.sortedWith(compareBy({ !it.ok }, { it.ms }))
            testPref.summary = ranked.mapIndexed { index, probe ->
                val label = labels.getOrNull(urls.indexOf(probe.url))?.takeIf { it.isNotBlank() }
                    ?: probe.url.toHostOrNull()
                    ?: probe.url
                val status = if (probe.ok) "✓ ${probe.ms} ms" else "✗ ${probe.detail ?: "failed"}"
                val suffix = when {
                    probe.url.toHostOrNull() == currentHost -> {
                        " (${context.stringResource(MR.strings.mirror_test_current)})"
                    }
                    index == 0 && probe.ok -> " (${context.stringResource(MR.strings.mirror_test_recommended)})"
                    else -> ""
                }
                "${index + 1}. $label · $status$suffix"
            }.joinToString("\n")
        }
    }

    private fun probeMirror(url: String): MirrorProbe {
        val start = SystemClock.elapsedRealtime()
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MIRROR_TEST_UA)
                .build()
            mirrorTestClient.newCall(request).execute().use { response ->
                MirrorProbe(
                    url = url,
                    ok = response.isSuccessful,
                    ms = SystemClock.elapsedRealtime() - start,
                    detail = if (response.isSuccessful) null else "HTTP ${response.code}",
                )
            }
        } catch (e: Exception) {
            MirrorProbe(
                url = url,
                ok = false,
                ms = SystemClock.elapsedRealtime() - start,
                detail = e.message?.take(60) ?: e::class.java.simpleName,
            )
        }
    }

    private fun String.toHostOrNull(): String? {
        val host = substringAfter("://", missingDelimiterValue = this).substringBefore('/')
        return if (host.matches(MIRROR_DOMAIN_REGEX)) host else null
    }

    private data class MirrorProbe(val url: String, val ok: Boolean, val ms: Long, val detail: String?)

    companion object {
        private const val SOURCE_ID = "source_id"

        private const val MIRROR_TEST_UA =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"

        private val MIRROR_DOMAIN_REGEX = Regex(
            "^[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$",
            RegexOption.IGNORE_CASE,
        )

        fun getInstance(sourceId: Long): SourcePreferencesFragment {
            return SourcePreferencesFragment().apply {
                arguments = Bundle().apply {
                    putLong(SOURCE_ID, sourceId)
                }
            }
        }
    }
}
