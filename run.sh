#!/bin/bash

# MatrixNet - Build and Run Script

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"

# Create output directory
mkdir -p "$OUT_DIR"

# Compile
echo -e "${YELLOW}Compiling...${NC}"
javac -d "$OUT_DIR" "$SRC_DIR"/*.java

if [ $? -ne 0 ]; then
    echo -e "${RED}Compilation failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Compilation successful!${NC}"

# Check arguments
if [ $# -lt 2 ]; then
    echo ""
    echo "Usage: ./run.sh <input_file> <output_file>"
    echo ""
    echo "Examples:"
    echo "  ./run.sh testcases/inputs/example.txt output.txt"
    echo "  ./run.sh testcases/inputs/type1_small.txt result.txt"
    echo ""
    echo "Available test cases:"
    ls -1 "$SCRIPT_DIR/testcases/inputs/" 2>/dev/null | sed 's/^/  - /'
    exit 0
fi

INPUT_FILE="$1"
OUTPUT_FILE="$2"

# Handle relative paths
if [[ ! "$INPUT_FILE" = /* ]]; then
    INPUT_FILE="$SCRIPT_DIR/$INPUT_FILE"
fi
if [[ ! "$OUTPUT_FILE" = /* ]]; then
    OUTPUT_FILE="$SCRIPT_DIR/$OUTPUT_FILE"
fi

# Run
echo -e "${YELLOW}Running...${NC}"
java -cp "$OUT_DIR" Main "$INPUT_FILE" "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Done! Output written to: $OUTPUT_FILE${NC}"
else
    echo -e "${RED}Execution failed!${NC}"
    exit 1
fi
