#!/bin/sh

read -s -p "Password: " PASS
keytool -genkey -v -keystore "../dist/keystore.jks" -storepass $PASS -keypass $PASS
