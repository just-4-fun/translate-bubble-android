package com.fortysevendeg.translatebubble.ui.preferences

import android.app.{AlertDialog, Activity}
import android.content.{Intent, DialogInterface}
import android.os.Bundle
import android.preference.Preference.{OnPreferenceChangeListener, OnPreferenceClickListener}
import android.preference._
import android.widget.Toast
import com.fortysevendeg.translatebubble.R
import com.fortysevendeg.macroid.extras.PreferencesBuildingExtra._
import com.fortysevendeg.macroid.extras.{AppContextProvider, RootPreferencesFragment}
import com.fortysevendeg.translatebubble.modules.ComponentRegistryImpl
import com.fortysevendeg.translatebubble.modules.clipboard.CopyToClipboardRequest
import com.fortysevendeg.translatebubble.modules.persistent.GetLanguagesRequest
import com.fortysevendeg.translatebubble.ui.bubbleservice.BubbleService
import com.fortysevendeg.translatebubble.ui.wizard.WizardActivity
import com.fortysevendeg.translatebubble.utils.{TranslateUIType, LanguageType}
import macroid.FullDsl._
import macroid.{AppContext, Contexts}

class MainActivity
    extends Activity
    with Contexts[Activity]
    with AppContextProvider {

  override implicit lazy val appContextProvider: AppContext = activityAppContext

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    BubbleService.launchIfIsNecessary
    getFragmentManager.beginTransaction.replace(android.R.id.content, new DefaultPreferencesFragment()).commit
  }

}

class DefaultPreferencesFragment
    extends PreferenceFragment
    with AppContextProvider
    with Contexts[PreferenceFragment]
    with ComponentRegistryImpl {

  override implicit lazy val appContextProvider: AppContext = fragmentAppContext

  implicit lazy val rootPreferencesFragment = new RootPreferencesFragment(this, R.xml.preferences)

  private lazy val launchFake = connect[PreferenceScreen]("launchFake")
  private lazy val typeTranslate = connect[ListPreference]("typeTranslate")
  private lazy val headUpNotification = connect[CheckBoxPreference]("headUpNotification")
  private lazy val toLanguage = connect[ListPreference]("toLanguage")
  private lazy val fromLanguage = connect[ListPreference]("fromLanguage")
  private lazy val openSource = connect[PreferenceScreen]("openSource")
  private lazy val showTutorial = connect[PreferenceScreen]("showTutorial")

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    // TODO Don't use 'map'. We should create a Tweak when MacroidExtra module works

    launchFake map (
      _.setOnPreferenceClickListener(new OnPreferenceClickListener {
        override def onPreferenceClick(preference: Preference): Boolean = {
          clipboardServices.copyToClipboard(CopyToClipboardRequest("Sample %d".format(System.currentTimeMillis())))
          true
        }
      })
    )

    showTutorial map (
      _.setOnPreferenceClickListener(new OnPreferenceClickListener {
        override def onPreferenceClick(preference: Preference): Boolean = {
          val intent = new Intent(getActivity, classOf[WizardActivity])
          val bundle = new Bundle()
          bundle.putBoolean(WizardActivity.keyModeTutorial, true)
          intent.putExtras(bundle)
          getActivity.startActivity(intent)
          true
        }
      })
    )

    openSource map (
      _.setOnPreferenceClickListener(new OnPreferenceClickListener {
        override def onPreferenceClick(preference: Preference): Boolean = {
          val builder = new AlertDialog.Builder(getActivity)
          builder.setMessage(R.string.openSourceMessage)
              .setPositiveButton(R.string.goToGitHub, new DialogInterface.OnClickListener() {
            def onClick(dialog: DialogInterface, id: Int) {
              // TODO Open github website project
              dialog.dismiss()
            }
          })
              .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            def onClick(dialog: DialogInterface, id: Int) {
              dialog.dismiss()
            }
          })
          val dialog = builder.create()
          dialog.show()
          true
        }
      })
    )

    typeTranslate map {
      translate =>
        setTypeTranslated(translate.getValue)
        val translates: List[String] = TranslateUIType.resourceNames
        val translatesValues: List[String] = TranslateUIType.stringNames()
        translate.setEntries(translates.toArray[CharSequence])
        translate.setEntryValues(translatesValues.toArray[CharSequence])
        translate.setOnPreferenceChangeListener(new OnPreferenceChangeListener {
          def onPreferenceChange(preference: Preference, newValue: AnyRef): Boolean = {
            setTypeTranslated(newValue.asInstanceOf[String])
            true
          }
        })
    }

    val languages: List[String] = LanguageType.resourceNames
    val languagesValues: List[String] = LanguageType.stringNames()

    fromLanguage map {
      from =>
        from.setEntries(languages.toArray[CharSequence])
        from.setEntryValues(languagesValues.toArray[CharSequence])
        from.setOnPreferenceChangeListener(new OnPreferenceChangeListener {
          def onPreferenceChange(preference: Preference, newValue: AnyRef): Boolean = {
            changeFrom(newValue.asInstanceOf[String])
            true
          }
        })
    }

    toLanguage map {
      to =>
        to.setEntries(languages.toArray[CharSequence])
        to.setEntryValues(languagesValues.toArray[CharSequence])
        to.setOnPreferenceChangeListener(new OnPreferenceChangeListener {
          def onPreferenceChange(preference: Preference, newValue: AnyRef): Boolean = {
            changeTo(newValue.asInstanceOf[String])
            true
          }
        })
    }

    persistentServices.getLanguages(GetLanguagesRequest()).mapUi(
      response => {
        changeFrom(response.from.toString)
        changeTo(response.to.toString)
      }
    )

  }

  private def setTypeTranslated(key: String) = {
    headUpNotification.map(_.setEnabled(key.equals(TranslateUIType.NOTIFICATION.toString)))
    typeTranslate map {
      translate =>
        key match {
          case _ if key.equals(TranslateUIType.NOTIFICATION.toString) =>
            translate.setTitle(R.string.notificationTitle)
            translate.setSummary(R.string.notificationMessage)
          case _ =>
            translate.setTitle(R.string.bubbleTitle)
            translate.setSummary(R.string.bubbleMessage)
        }
    }
  }

  private def changeTo(key: String) = {
    val toNameLang: String = getString(getResources.getIdentifier(key, "string", getActivity.getPackageName))
    toLanguage map (_.setTitle(getString(R.string.to, toNameLang)))
  }

  private def changeFrom(key: String) = {
    val fromNameLang: String = getString(getResources.getIdentifier(key, "string", getActivity.getPackageName))
    fromLanguage map (_.setTitle(getString(R.string.from, fromNameLang)))
  }

}