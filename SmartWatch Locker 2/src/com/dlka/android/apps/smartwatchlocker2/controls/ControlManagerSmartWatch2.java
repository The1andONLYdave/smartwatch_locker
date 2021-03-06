/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2011-2013, Sony Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB / Sony Mobile
 Communications AB nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.dlka.android.apps.smartwatchlocker2.controls;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dlka.android.apps.smartwatchlocker2.R;
import com.dlka.android.apps.smartwatchlocker2.SampleExtensionService;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlListItem;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

/**
 * The phone control manager manages which control to currently show on the
 * display. This class then forwards any life-cycle methods and events events to
 * the running control. This class handles API level 2 methods and an Intent
 * based ControlExtension history stack
 */
public class ControlManagerSmartWatch2 extends ControlManagerBase {

    private Stack<Intent> mControlStack;

    public ControlManagerSmartWatch2(Context context, String packageName) {
        super(context, packageName);
        mControlStack = new Stack<Intent>();
        // Create an initial control extension
        Intent initialListControlIntent = new Intent(mContext, ListControlExtension.class);
        mCurrentControl = createControl(initialListControlIntent);
    }

    /**
     * Get supported control width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
    }

    /**
     * Get supported control height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_height);
    }

    @Override
    public void onRequestListItem(int layoutReference, int listItemPosition) {
        Log.v(SampleExtensionService.LOG_TAG, "onRequestListItem");
        if (mCurrentControl != null) {
            mCurrentControl.onRequestListItem(layoutReference, listItemPosition);
        }
    }

    @Override
    public void onListItemClick(ControlListItem listItem, int clickType, int itemLayoutReference) {
        Log.v(SampleExtensionService.LOG_TAG, "onListItemClick");
        if (mCurrentControl != null) {
            mCurrentControl.onListItemClick(listItem, clickType, itemLayoutReference);
        }
    }

    @Override
    public void onListItemSelected(ControlListItem listItem) {
        Log.v(SampleExtensionService.LOG_TAG, "onListItemSelected");
        if (mCurrentControl != null) {
            mCurrentControl.onListItemSelected(listItem);
        }
    }

    @Override
    public void onListRefreshRequest(int layoutReference) {
        Log.v(SampleExtensionService.LOG_TAG, "onListRefreshRequest");
        if (mCurrentControl != null) {
            mCurrentControl.onListRefreshRequest(layoutReference);
        }
    }

    @Override
    public void onObjectClick(ControlObjectClickEvent event) {
        Log.v(SampleExtensionService.LOG_TAG, "onObjectClick");
        if (mCurrentControl != null) {
            mCurrentControl.onObjectClick(event);
        }
    }

    @Override
    public void onKey(int action, int keyCode, long timeStamp) {
        Log.v(SampleExtensionService.LOG_TAG, "onKey");

        if (action == Control.Intents.KEY_ACTION_RELEASE
                && keyCode == Control.KeyCodes.KEYCODE_BACK) {
            Log.d(SampleExtensionService.LOG_TAG, "onKey() - back button intercepted.");
            onBack();
        } else if (mCurrentControl != null) {
            super.onKey(action, keyCode, timeStamp);
        }
    }

    @Override
    public void onMenuItemSelected(int menuItem) {
        Log.v(SampleExtensionService.LOG_TAG, "onMenuItemSelected");
        if (mCurrentControl != null) {
            mCurrentControl.onMenuItemSelected(menuItem);
        }
    }

    /**
     * Closes the currently open control extension. If there is a control on the
     * back stack it is opened, otherwise extension is closed.
     */
    public void onBack() {
        Log.v(SampleExtensionService.LOG_TAG, "onBack");
        if (!mControlStack.isEmpty()) {
            Intent backControl = mControlStack.pop();
            ControlExtension newControl = createControl(backControl);
            startControl(newControl);
        } else {
            stopRequest();
        }
    }

    /**
     * Start a new control. Any currently running control will be stopped and
     * put on the control extension stack.
     *
     * @param intent the Intent used to create the ManagedControlExtension. The
     *            intent must have the requested ManagedControlExtension as
     *            component, e.g. Intent intent = new Intent(mContext,
     *            CallLogDetailsControl.class);
     */
    public void startControl(Intent intent) {
        addCurrentToControlStack();
        ControlExtension newControl = createControl(intent);
        startControl(newControl);
    }

    public void addCurrentToControlStack() {
        if (mCurrentControl != null && mCurrentControl instanceof ManagedControlExtension) {
            Intent intent = ((ManagedControlExtension) mCurrentControl).getIntent();
            boolean isNoHistory = intent.getBooleanExtra(
                    ManagedControlExtension.EXTENSION_NO_HISTORY,
                    false);
            if (isNoHistory) {
                // Not adding this control to history
                Log.d(SampleExtensionService.LOG_TAG, "Not adding control to history stack");
            } else {
                Log.d(SampleExtensionService.LOG_TAG, "Adding control to history stack");
                mControlStack.add(intent);
            }
        } else {
            Log.w(SampleExtensionService.LOG_TAG,
                    "ControlManageronly supports ManagedControlExtensions");
        }
    }

    private ControlExtension createControl(Intent intent) {
        ComponentName component = intent.getComponent();
        try {
            String className = component.getClassName();
            Log.d(SampleExtensionService.LOG_TAG, "Class name:" + className);
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(Context.class, String.class,
                    ControlManagerSmartWatch2.class, Intent.class);
            if (ctor == null) {
                return null;
            }
            Object object = ctor.newInstance(new Object[] {
                    mContext, mHostAppPackageName, this, intent
            });
            if (object instanceof ManagedControlExtension) {
                return (ManagedControlExtension) object;
            } else {
                Log.w(SampleExtensionService.LOG_TAG,
                        "Created object not a ManagedControlException");
            }

        } catch (SecurityException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (NoSuchMethodException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (IllegalArgumentException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (InstantiationException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (IllegalAccessException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (InvocationTargetException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        } catch (ClassNotFoundException e) {
            Log.w(SampleExtensionService.LOG_TAG, "ControlManager: Failed in creating control", e);
        }
        return null;
    }

}
