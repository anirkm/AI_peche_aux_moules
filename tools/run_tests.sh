#!/bin/bash
set -euo pipefail

# Script de tests (presets + seeds).
# Tout est relance a chaque run (logs + resultats).

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IP=${1:-127.0.0.1}
PORT=${2:-1337}
EQUIPE=${3:-test}
SEEDS_FILE=${4:-}
AUTO_SERVER=${AUTO_SERVER:-0}

LOG_DIR="$ROOT/IA/superAI/logs"
mkdir -p "$LOG_DIR"
rm -f "$LOG_DIR"/test*.txt
echo "Info: logs test*.txt, logs/seed-* et tools/results.csv seront ecrases."

# Compile l'IA
javac -d "$ROOT/IA" "$ROOT"/IA/superAI/*.java

run_preset() {
  local name="$1"
  shift
  echo "=== Lancement preset $name ==="
  local log_file="$LOG_DIR/test${name}.txt"
  rm -f "$log_file"
  if [[ "${AUTO_SERVER:-0}" -eq 1 && -n "${CURRENT_SEED_LABY:-}" ]]; then
    start_server "$CURRENT_SEED_LABY" "$CURRENT_SEED_BONUS"
  fi
  java -cp "$ROOT/IA" superAI.ClientSuperAI "$IP" "$PORT" "$EQUIPE" log \
    "logFichier=$log_file" \
    "$@"
  if [[ "${AUTO_SERVER:-0}" -eq 1 && -n "${CURRENT_SEED_LABY:-}" ]]; then
    stop_server
  fi
  if ! rg -q "^Tour " "$log_file" 2>/dev/null; then
    echo "Erreur: pas de tours ecrits pour le preset $name (serveur non joignable ?)"
    exit 1
  fi
}

start_server() {
  local seedLaby="$1"
  local seedBonus="$2"
  local seedId="${seedLaby}-${seedBonus}"
  local server_log="$LOG_DIR/server-${seedId}.log"

  if [[ ! -f "$ROOT/Serveur/PecheAuxMoulesBoucle.class" ]]; then
    (cd "$ROOT/Serveur" && javac -cp . PecheAuxMoulesBoucle.java)
  fi

  echo "=== Demarrage serveur seed ${seedId} ==="
  (
    cd "$ROOT/Serveur" || exit 1
    exec java -cp . PecheAuxMoulesBoucle \
      -nbJoueur 1 -delay 0 -timeout 3000 \
      -numLaby "$seedLaby" -numPlacementBonus "$seedBonus"
  ) > "$server_log" 2>&1 &
  SERVER_PID=$!
  wait_for_server "$server_log"
}

stop_server() {
  if [[ -n "${SERVER_PID:-}" ]]; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" >/dev/null 2>&1 || true
    SERVER_PID=""
  fi
}

wait_for_server() {
  local server_log="$1"
  local ok=0
  for _ in $(seq 1 40); do
    if (echo > /dev/tcp/"$IP"/"$PORT") >/dev/null 2>&1; then
      ok=1
      break
    fi
    sleep 0.2
  done
  if [[ "$ok" -ne 1 ]]; then
    echo "Serveur non joignable sur ${IP}:${PORT}."
    if [[ -f "$server_log" ]]; then
      echo "Dernieres lignes du log serveur:"
      tail -n 20 "$server_log"
    fi
    stop_server
    exit 1
  fi
}

run_all_presets() {
  # Base G1B0 (preset par défaut)
  local base_g1=(
    modeHybride=1 seuilAcceleration=12 seuilRarete=3
    valeurSaut=25 valeurTroisPas=35
    penaliteUtilisationSaut=10 penaliteUtilisationTroisPas=14
    penaliteDistance=1.6 coeffProfondeur=0.7
    penaliteCibleAdverse=0.0 coeffCarteValeur=0.0 bonusUsageEfficace=0.0
    verrouillageCible=4 seuilChangementCible=1.3
    toursSansPointsMax=12 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2
    modePlan=1 nbCiblesPlan=8 profondeurPlan=4
    modeBeam=0 largeurBeam=12 profondeurBeam=3
  )

  # G1B0 de base
  run_preset G1B0 "${base_g1[@]}"

  # G1B0+ (poids plus forts pour adversaires/carte/bonus)
  run_preset G1B0P "${base_g1[@]}" \
    penaliteCibleAdverse=12 coeffCarteValeur=0.15 bonusUsageEfficace=4

  # G1AUTO (beam auto)
  run_preset G1AUTO "${base_g1[@]}" modeBeam=2

  # (presets de beam profonds retirés pour garder un set court)
}

if [[ -z "$SEEDS_FILE" && -f "$ROOT/tools/seeds.txt" ]]; then
  SEEDS_FILE="$ROOT/tools/seeds.txt"
fi

if [[ -n "$SEEDS_FILE" ]]; then
  RESULTS="$ROOT/tools/results.csv"
  echo "seed,preset,tours,points" > "$RESULTS"
  rm -rf "$ROOT/IA/superAI/logs/seed-"*

  while read -r seedLaby seedBonus; do
    [[ -z "$seedLaby" ]] && continue
    seedId="${seedLaby}-${seedBonus}"
    LOG_DIR="$ROOT/IA/superAI/logs/seed-${seedId}"
    mkdir -p "$LOG_DIR"
    rm -f "$LOG_DIR"/test*.txt
    echo "=== Seed ${seedId} ==="
    if [[ "$AUTO_SERVER" -eq 1 ]]; then
      CURRENT_SEED_LABY="$seedLaby"
      CURRENT_SEED_BONUS="$seedBonus"
    else
      echo "Lance le serveur avec : -numLaby ${seedLaby} -numPlacementBonus ${seedBonus}"
      echo "Puis appuie sur Entrée."
      read -r
    fi
    run_all_presets
    python3 "$ROOT/tools/analyse_logs.py" "$LOG_DIR"/test*.txt
    python3 "$ROOT/tools/collect_results.py" "$seedId" "$LOG_DIR"/test*.txt >> "$RESULTS"
  done < "$SEEDS_FILE"

  python3 "$ROOT/tools/aggregate_results.py" "$RESULTS"
  exit 0
fi

run_all_presets
python3 "$ROOT/tools/analyse_logs.py" "$LOG_DIR"/test*.txt
