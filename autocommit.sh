#! /bin/bash

read -p "Commit message: " message

git add .
git stage .
git commit -m "$message"
git push