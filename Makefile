SHELL := /bin/bash

GRADLE ?= ./gradlew
ADB ?= adb
PYTHON ?= python3
APP_DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help message.
	@awk 'BEGIN {FS = ":.*##"; printf "\nDroidJax make targets:\n\n"} /^[a-zA-Z0-9_.-]+:.*##/ {printf "  %-28s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.PHONY: test
test: ## Run all unit tests.
	$(GRADLE) test

.PHONY: check
check: ## Run the standard verification suite.
	$(GRADLE) test assembleDebug

.PHONY: build
build: ## Build debug artifacts for all Android modules.
	$(GRADLE) assembleDebug

.PHONY: clean
clean: ## Remove Gradle build outputs.
	$(GRADLE) clean

.PHONY: core-test
core-test: ## Run pure Kotlin core tests.
	$(GRADLE) :core:test

.PHONY: android-common-test
android-common-test: ## Run android-common local unit tests.
	$(GRADLE) :android-common:testDebugUnitTest

.PHONY: floating-build
floating-build: ## Compile the floating-helper debug module.
	$(GRADLE) :floating-helper:compileDebugKotlin

.PHONY: ime-build
ime-build: ## Compile the keyboard-ime debug module.
	$(GRADLE) :keyboard-ime:compileDebugKotlin

.PHONY: app-build
app-build: ## Assemble the debug app APK.
	$(GRADLE) :app:assembleDebug

.PHONY: install-debug
install-debug: app-build ## Install the debug app APK on the connected device.
	$(ADB) install -r $(APP_DEBUG_APK)

.PHONY: uninstall
uninstall: ## Uninstall the debug app from the connected device.
	-$(ADB) uninstall com.droidjax.app

.PHONY: devices
devices: ## List connected Android devices.
	$(ADB) devices

.PHONY: mathjax-test-server
mathjax-test-server: ## Serve the phone-friendly MathJax IME test page.
	$(PYTHON) tools/mathjax-test/server.py
