CWAC RichEditText: Letting Users Make Text Pretty
=================================================

Android's `EditText` widget supports formatted (a.k.a.,
"rich text") editing. It just lacks any way for the user
to supply formatting, and it does not provide much in the
way of convenience methods for a developer to, say, tie
in some sort of toolbar to allow users to format selections.

That's where `RichEditText` comes in.

`RichEditText` is a drop-in replacement for `EditText` that:

- Provides an action mode on Android 4.0+ that allows
users to format selected pieces of text
- Provides convenience methods to allow developers to 
trigger formatting for selected text via other means

This widget is packaged as an Android library project, with
a `demo/` subdirectory containing a regular Android project
with a couple of activities demonstrating the use of
`RichEditText`.

In addition to the documentation on this page,
[partial JavaDocs are also available](http://javadocs.commonsware.com/cwac/richedit/index.html).

This Android library project is available as an artifact for use
with Gradle. To use that, add the following
blocks to your `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://repo.commonsware.com.s3.amazonaws.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:richedit:0.3.0'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

If you are not using Gradle, download or clone this repo, and add the `richedit/`
project to your environment as an Android library project.

**NOTE**: If you were using v0.2.0 with ActionBarSherlock, ActionBarSherlock
support was removed from this project as of v0.3.0. Please remain on v0.2.0,
or switch to the native API Level 11+ action bar (a.k.a., "15 is the new 10").

Usage
-----
Simply add `com.commonsware.cwac.richedit.RichEditText`
widgets to your layout as needed:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.commonsware.cwac.richedit.RichEditText xmlns:android="http://schemas.android.com/apk/res/android"
  android:id="@+id/editor"
  android:layout_width="fill_parent"
  android:layout_height="fill_parent"
  android:gravity="top|left"
  android:inputType="textMultiLine">

  <requestFocus/>

</com.commonsware.cwac.richedit.RichEditText>
```

At this time, there are no custom attributes used by
`RichEditText`.

On its own, by default, `RichEditText` provides one means of users
applying formatting: the standard `<Ctrl>-<B>` for bold,
`<Ctrl>-<I>` for italics, and `<Ctrl>-<U>` for underline work if there
is a selection. You can disable this by calling
`setKeyboardShortcutsEnabled(false)`.

If you want an on-screen UI for formatting, you have two choices.

First, you can call
`enableActionModes()` on the `RichEditText`. This will add a "FORMAT"
entry on the action mode that comes up when the user highlights some
prose in the editor. Tapping that will allow the user to toggle various
effects. 

The action modes work so-so on phones at this time &mdash;
tablets work better. To get it to work on phones at all, you will need
to include `android:imeOptions="flagNoExtractUi"` as an attribute on the
`RichEditText`.

Alternatively, you can have
your own toolbar or gesture interface or
whatever to allow users to format text. In that case, here are the two key
methods to call on `RichEditText`:

- `applyEffect()` changes the current selection, applying
or removing an effect (e.g., making the selection bold). The
first parameter is the effect to apply (e.g., `RichEditText.BOLD`).
The second parameter is the new value for the effect. Many
effects take boolean values, so `applyEffect(RichEditText.BOLD, true)`
would format the current selection as bold.

- `setOnSelectionChangedListener()` is where you register a
`RichEditText.OnSelectionChangedListener` object, which will
be called with `onSelectionChanged()` whenever the user changes
the selection in the widget (i.e., highlights text or taps
to un-select the highlight). You are provided the start and
end positions of the selection (as were supplied to `onSelectionChanged()`
to `RichEditText` itself by Android), plus a list of effects
that are active on that selection. This will allow you to 
update your toolbar to indicate what is and is not in use,
and so you know what to do when the user taps on one of
those toolbar buttons again.

### Supported Effects

At the time of this writing, here are the `RichEditText`
static data members for each supported effect:

- `BOLD`
- `ITALIC`
- `UNDERLINE`
- `STRIKETHROUGH`
- `SUPERSCRIPT`
- `SUBSCRIPT`
- `TYPEFACE`

There are other effects presently implemented, but they
will be revised shortly, including name and data type
changes, so don't mess with them yet.

Dependencies
------------
There are no third-party dependencies at this time.

This project should work on API Level 7 and higher, except for any portions that
may be noted otherwise in this document. Please report bugs if you find features
that do not work on API Level 7 and are not noted as requiring a higher version.

Version
-------
This is version v0.3.1 of this module, meaning it is out of its years-long
hibernation and is ready to rampage through downtown San Francisco (or other
cities if you prefer).

Demo
----
In the `demo/` sub-project you will find
a sample activity that demonstrates the use of `RichEditor`.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with `commonsware-cwac`
and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[android]+richedittext).
Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-richedit/issues).
Be certain to include complete steps for reproducing the issue.

Do not ask for help via Twitter.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.

Release Notes
-------------
- v0.3.1: updated for Android Studio 1.0 and new AAR publishing system
- v0.3.0: removed ActionBarSherlock support, icon for FORMAT action mode item, fixed clipboard bug, added Gradle support
- v0.2.0: added keyboard shortcuts for bold/italic/underline and test suite, bug fixes
- v0.1.1: added `disableActionModes()` and fixed bug related to conditional action mode usage
- v0.1: added action mode support using ActionBarSherlock for pre-Honeycomb devices
- v0.0.3: removed `RichEditor`, replaced it with custom action modes
- v0.0.2: added `RichEditor` and made various fixes
- v0.0.1: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

