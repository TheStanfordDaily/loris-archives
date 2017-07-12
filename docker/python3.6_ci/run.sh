#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

cd /data

if [[ "$OP" == "lint" ]]
then
  echo "Linting Lambdas"
  flake8 .
elif [[ "$OP" == "test" ]]
then
  echo "Testing Lambdas"
  ./install_lambda_deps.sh
  find **/test_*.py | py.test
elif [[ "$OP" == "install-deps" ]]
then
  echo "Installing Lambda dependencies"
  ./install_lambda_deps.sh
else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
