package com.habitrpg.android.habitica.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.components.AppComponent
import com.habitrpg.android.habitica.data.SocialRepository
import com.habitrpg.android.habitica.data.UserRepository
import com.habitrpg.android.habitica.extensions.notNull
import com.habitrpg.android.habitica.extensions.runDelayed
import com.habitrpg.android.habitica.helpers.RxErrorHandler
import com.habitrpg.android.habitica.models.user.User
import com.habitrpg.android.habitica.modules.AppModule
import com.habitrpg.android.habitica.prefs.scanner.IntentIntegrator
import com.habitrpg.android.habitica.ui.fragments.social.party.PartyInviteFragment
import com.habitrpg.android.habitica.ui.helpers.bindView
import com.habitrpg.android.habitica.ui.helpers.dismissKeyboard
import com.habitrpg.android.habitica.ui.views.HabiticaSnackbar.Companion.showSnackbar
import com.habitrpg.android.habitica.ui.views.HabiticaSnackbar.SnackbarDisplayType
import io.reactivex.functions.Consumer
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class GroupInviteActivity : BaseActivity() {

    @field:[Inject Named(AppModule.NAMED_USER_ID)]
    lateinit var userId: String
    @Inject
    lateinit var socialRepository: SocialRepository
    @Inject
    lateinit var userRepository: UserRepository

    internal val tabLayout: TabLayout by bindView(R.id.tab_layout)
    internal val viewPager: ViewPager by bindView(R.id.viewPager)
    private val snackbarView: ViewGroup by bindView(R.id.snackbar_view)


    internal var fragments: MutableList<PartyInviteFragment> = ArrayList()
    private var userIdToInvite: String? = null

    override fun getLayoutResId(): Int {
        return R.layout.activity_party_invite
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("onCreate XX", "beginning of onCreate")
        super.onCreate(savedInstanceState)
        viewPager.currentItem = 0

        if (intent.getStringExtra("groupType") == "party") {
            supportActionBar?.title = getString(R.string.invite_to_party)
        } else {
            supportActionBar?.title = getString(R.string.invite_to_guild)
        }

        setViewPagerAdapter()
        Log.d("onCreate XX", "end of onCreate")
    }

    override fun injectActivity(component: AppComponent?) {
        Log.d("injectActivity XX", "beginning of injectActivity")
        component?.inject(this)
        Log.d("injectActivity XX", "end of injectActivity")

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d("onCreateOptionsMenu XX", "beginning of onCreateOptionsMenu")
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_party_invite, menu)
        Log.d("onCreateOptionsMenu XX", "end of onCreateOptionsMenu")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("onOptionsItemSelectedXX", "beginning of onOptionsItemSelected")

        val id = item.itemId

        if (id == R.id.action_send_invites) {
            setResult(Activity.RESULT_OK, createResultIntent())
            showConfirmation()
            dismissKeyboard()
            Log.d("onOptionsItemSelectedXX", "end of onOptionsItemSelected")
            return true
        }
        Log.d("onOptionsItemSelectedXX", "end of onOptionsItemSelected")
        return super.onOptionsItemSelected(item)
    }

    private fun createResultIntent(): Intent {
        //tried, crashed with ID error
        Log.d("createResultIntent XX", "beginning of createResultIntent")

        val intent = Intent()
        val fragment = fragments[viewPager.currentItem]
        if (viewPager.currentItem == 1) {
            intent.putExtra(IS_EMAIL_KEY, true)
            intent.putExtra(EMAILS_KEY, fragment.values)
        } else {
            intent.putExtra(IS_EMAIL_KEY, false)
            intent.putExtra(USER_IDS_KEY, fragment.values)
        }

        Log.d("createResultIntent XX", "end of createResultIntent")
        return intent
    }

    private fun showConfirmation() {
        Log.d("showConfirmation XX", "beginning of showConfirmation")
        //display snackbar
        showSnackbar(snackbarView, "Invite Sent!", SnackbarDisplayType.SUCCESS)
        Log.d("showConfirmation XX", "end of showConfirmation")
    }

    private fun setViewPagerAdapter() {
        Log.d("setViewPagerAdapter XX", "beginning of setViewPagerAdapter")

        val fragmentManager = supportFragmentManager

        viewPager.adapter = object : FragmentPagerAdapter(fragmentManager) {

            override fun getItem(position: Int): Fragment {

                val fragment = PartyInviteFragment()
                fragment.isEmailInvite = position == 1
                if (fragments.size > position) {
                    fragments[position] = fragment
                } else {
                    fragments.add(fragment)
                }
                Log.d("setViewPagerAdapter XX", "end of setViewPagerAdapter")

                return fragment
            }

            override fun getCount(): Int {
                Log.d("setViewPagerAdapter XX", "end of setViewPagerAdapter")
                return 2
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position) {
                    0 -> getString(R.string.invite_existing_users)
                    1 -> getString(R.string.by_email)
                    else -> ""
                }
            }
        }
        Log.d("setViewPagerAdapter XX", "end of setViewPagerAdapter")
        tabLayout.setupWithViewPager(viewPager)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("onActivityResult XX", "beginning of onActivityResult, before showConfirmation")
        //showConfirmation()
        super.onActivityResult(requestCode, resultCode, data)

        val scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (scanningResult != null && scanningResult.contents != null) {
            val qrCodeUrl = scanningResult.contents
            val uri = qrCodeUrl.toUri()
            if (uri.pathSegments.size < 3) {
                Log.d("onActivityResult XX", "end of onActivityResult")

                return
            }
            userIdToInvite = uri.pathSegments[2]

            compositeSubscription.add(userRepository.getUser(userId).subscribe(Consumer<User> { this.handleUserReceived(it) }, RxErrorHandler.handleEmptyError()))
        }
        Log.d("onActivityResult XX", "end of onActivityResult")
    }

    private fun handleUserReceived(user: User) {
        Log.d("handleUserReceived XX", "beginning of handleUserReceived")

        //this method doesn't get called
        if (this.userIdToInvite == null) {
            return
        }

        val toast = Toast.makeText(applicationContext,
                "Invited: $userIdToInvite", Toast.LENGTH_LONG)
        toast.show()

        val inviteData = HashMap<String, Any>()
        val invites = ArrayList<String>()
        userIdToInvite.notNull {
            invites.add(it)
        }
        inviteData["uuids"] = invites

        compositeSubscription.add(this.socialRepository.inviteToGroup(user.party?.id
                ?: "", inviteData)
                .subscribe(Consumer { }, RxErrorHandler.handleEmptyError()))
        Log.d("handleUserReceived XX", "end of handleUserReceived")

    }

    companion object {

        const val RESULT_SEND_INVITES = 100
        const val USER_IDS_KEY = "userIDs"
        const val IS_EMAIL_KEY = "isEmail"
        const val EMAILS_KEY = "emails"
    }
}
