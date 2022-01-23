#!/bin/bash
BWHT="\033[1;37m"
REG="(tput sgr0)"
LIB=../../lib/*:../
CLI=client.ClientMain

clients=(
    'java -cp '$LIB' '$CLI' "reward notification|register u21 pas21 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u22 pas22 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u23 pas23 arte teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u24 pas24 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u25 pas25 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u26 pas26 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u27 pas27 arte teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u28 pas28 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u29 pas29 arte sport tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u30 pas30 politica"'
    )

i=0

until [ $i -gt ${#clients[@]} ]
do
  echo ${clients[i]} $BASHPID
  ${clients[i]}
  ((i=i+1))
done


# i=$(( RANDOM % ${#clients[@]}))
# echo ${clients[1]}
# ${clients[1]}
