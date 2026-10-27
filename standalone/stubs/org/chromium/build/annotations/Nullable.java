// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stand-in for Chromium's annotation so the pure-Java Zinc core compiles outside a Chromium
 * checkout. Inside Chromium the real //build/android:build_java annotation is used instead.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
public @interface Nullable {}
