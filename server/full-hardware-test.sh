#!/bin/bash
echo "Starting hardware test in 10 seconds. Please watch fermbot server logs for instructions and expected outcomes"
sleep 10
curl -H "Content-type: application/json" -d '{"stepDuration":"PT10S"}' 'http://localhost:8080/test/full-hardware'
echo "Hardware test complete"