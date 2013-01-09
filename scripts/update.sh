#!/bin/sh
json='{"id":123,"cat":["ABC", "CDE"],"skill":["abc","cde"]}'
curl -H "Content-Type: application/json" -d "$json" http://127.0.0.1:8000/update
