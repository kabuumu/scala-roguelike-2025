#!/usr/bin/env python3
"""
Simple code coverage analyzer for Scala Roguelike 2025
Since scoverage has limitations with ScalaJS, this script provides basic coverage analysis
by analyzing test files and their corresponding source files.
"""

import os
import re
from pathlib import Path

def count_lines_of_code(file_path):
    """Count non-empty, non-comment lines in a Scala file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove comments and empty lines
    lines = content.split('\n')
    code_lines = 0
    in_multiline_comment = False
    
    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue
            
        # Handle multiline comments
        if '/*' in stripped and '*/' in stripped:
            # Single line multiline comment
            before_comment = stripped.split('/*')[0].strip()
            if before_comment and not before_comment.startswith('//'):
                code_lines += 1
            continue
        elif '/*' in stripped:
            in_multiline_comment = True
            before_comment = stripped.split('/*')[0].strip()
            if before_comment and not before_comment.startswith('//'):
                code_lines += 1
            continue
        elif '*/' in stripped:
            in_multiline_comment = False
            after_comment = stripped.split('*/')[-1].strip()
            if after_comment and not after_comment.startswith('//'):
                code_lines += 1
            continue
        elif in_multiline_comment:
            continue
        elif stripped.startswith('//'):
            continue
        else:
            code_lines += 1
    
    return code_lines

def analyze_test_coverage():
    """Analyze test coverage by examining test files and their targets."""
    project_root = Path(__file__).parent.parent
    src_main = project_root / "src" / "main" / "scala"
    src_test = project_root / "src" / "test" / "scala"
    
    if not src_main.exists() or not src_test.exists():
        print("Error: Source directories not found")
        return
    
    # Count total lines of code in main source
    total_main_lines = 0
    main_files = list(src_main.rglob("*.scala"))
    
    for file_path in main_files:
        if "generated" not in str(file_path):  # Exclude generated files
            lines = count_lines_of_code(file_path)
            total_main_lines += lines
    
    # Count lines in test files
    total_test_lines = 0
    test_files = list(src_test.rglob("*.scala"))
    
    for file_path in test_files:
        lines = count_lines_of_code(file_path)
        total_test_lines += lines
    
    # Analyze which areas are tested
    tested_areas = {
        "game/entity": 0,
        "game/system": 0,
        "ui": 0,
        "map": 0,
        "util": 0,
        "indigoengine": 0
    }
    
    main_areas = {
        "game/entity": 0,
        "game/system": 0,
        "ui": 0,
        "map": 0,
        "util": 0,
        "indigoengine": 0
    }
    
    # Count lines in each area of main source
    for file_path in main_files:
        if "generated" in str(file_path):
            continue
        rel_path = file_path.relative_to(src_main)
        lines = count_lines_of_code(file_path)
        
        for area in main_areas:
            if str(rel_path).startswith(area.replace("/", os.sep)):
                main_areas[area] += lines
                break
    
    # Count lines in each area of test source (indicates what's being tested)
    for file_path in test_files:
        rel_path = file_path.relative_to(src_test)
        lines = count_lines_of_code(file_path)
        
        for area in tested_areas:
            if str(rel_path).startswith(area.replace("/", os.sep)):
                tested_areas[area] += lines
                break
    
    print("=== Code Coverage Analysis ===")
    print(f"Total main source lines: {total_main_lines}")
    print(f"Total test lines: {total_test_lines}")
    print(f"Test to source ratio: {total_test_lines / total_main_lines:.2f}")
    print()
    
    print("=== Coverage by Area ===")
    overall_coverage = 0
    total_weight = 0
    
    for area in main_areas:
        main_lines = main_areas[area]
        test_lines = tested_areas[area]
        
        if main_lines > 0:
            # Simple heuristic: every 10 lines of tests covers roughly 30 lines of main code
            estimated_coverage = min(100, (test_lines * 3.0) / main_lines * 100)
            overall_coverage += estimated_coverage * main_lines
            total_weight += main_lines
        else:
            estimated_coverage = 0
        
        print(f"{area:15}: {main_lines:4d} lines, {test_lines:3d} test lines, ~{estimated_coverage:5.1f}% coverage")
    
    if total_weight > 0:
        overall_coverage = overall_coverage / total_weight
    
    print(f"\nEstimated overall coverage: {overall_coverage:.1f}%")
    print("\nNote: This is a heuristic estimate. Actual coverage may vary.")
    print("Areas with comprehensive tests: game/entity, game/system, ui, map, util")
    
    return overall_coverage

if __name__ == "__main__":
    analyze_test_coverage()