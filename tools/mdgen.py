#!/usr/bin/env python

# Simple parser to generate markdown files from templates
import argparse
import re


def replaceInclude(obj):
	"""Reads the given file to include its contents"""
	
	txt = file(obj.groups()[0], "r").read()
	return txt

def parseMD(intxt):
	"""Parse an given template string and returns the new markdown file"""

	intxt = re.sub("\{include:\s*([^\}]+)\s*\}", replaceInclude, intxt)
	return intxt

if __name__ == "__main__":

	# Define CLI options
	parser = argparse.ArgumentParser(description="Generate markdown files from templates")
	parser.add_argument("templates", type=argparse.FileType('r'), nargs="+", help="Input files")

	# Read the parameters and use them
	args = parser.parse_args()
	for fileobj in args.templates:
		print parseMD(fileobj.read())
