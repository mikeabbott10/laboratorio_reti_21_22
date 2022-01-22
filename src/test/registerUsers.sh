#!/bin/bash
BWHT="\033[1;37m"
REG="(tput sgr0)"
LIB=../../lib/*:../
CLI=client.ClientMain

clients=(
    'java -cp '$LIB' '$CLI' "reward notification|register u1 pas1 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u2 pas2 teatro"'
    'java -cp '$LIB' '$CLI' "reward notification|register u3 pas3 arte teatro"'
    'java -cp '$LIB' '$CLI' "reward notification|register u4 pas4 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u5 pas5 arte sport"'
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
