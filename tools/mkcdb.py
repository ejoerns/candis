#!/usr/bin/env python

import argparse
import os
import re
import subprocess
import tempfile
import zipfile
import sys

def execute(cmd, shell=True, cwd=None):
	p = subprocess.Popen(cmd, shell=shell, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
	while True:
		line = p.stdout.readline()
		if line == "" and p.poll() != None:
			break
		sys.stdout.write(line)
		sys.stdout.flush()
	output = p.communicate()[0]
	exit_code = p.returncode
	if exit_code == 0:
		return output
	else:
		print (cmd, exit_code, output)

def mkdex(dexfile, infiles):
	android_home = os.environ.get("ANDROID_HOME")
	if not android_home:
		raise EnvironmentError("Environment variable ANDROID_HOME not set")
	if not os.path.exists(android_home):
		raise EnvironmentError("Path \"%s\" not found" % android_home)

	dexbin = "%s/platform-tools/dx" % (android_home)
	if not os.path.exists(dexbin):
		dexbin = "%s/bin/dx" % (android_home)
	if not os.path.exists(dexbin):
		raise IOError(dexbin)
	
	cmd = "%s --dex --verbose --output=\"%s\" %s" % (dexbin, dexfile, " ".join(map(lambda f: "\"%s\"" % f, infiles)))
	print ">>", cmd
	execute(cmd)

def ant(directory, targets):
	"""Run ant in a specified directory with some targets"""
	cmd = "ant %s" % " ".join(targets)
	print ">>", cmd
	execute(cmd, cwd=directory)


class JavaProperties:
	def __init__(self, filename):
		self._store = {}
		data = file(filename, "r").read()
		follow = False
		key = ""
		values = []
		for line in data.splitlines():
			line = line.strip()
			if not line or line.startswith("#"):
				continue


			if follow:
				values.append(line.rstrip("\\"))
			else:
				if key:
					self.addEntry(key, values)
				values = []
				key, v = line.split("=", 1)
				values.append(v.rstrip("\\"))
			follow = line.endswith("\\")
		if key:
			self.addEntry(key, values)

	def addEntry(self, key, values):
		self._store[key] = "".join(values)

	def parseValue(self, value):
		def repl(match):
			values = self.getEntry(match.groups()[0])
			if values:
				return values
			return ""
		value = re.sub("\$\{([^\}]*)\}", repl, value)
		return value
			

	def getEntry(self, key):
		if key in self._store:
			return self.parseValue(self._store[key])
		return None


class JavaProject:
	def __init__(self, path):
		self.libs = []
		self.main = None
		self.name = None
		self.path = path
		if self.__isNetbeans(path):
			self.__loadNetbeans(path)
			self.targets = ["jar", "jar"]
		elif self.__isNbAndroid(path):
			self.__loadNbAndroid(path)
			self.targets = ["debug", "release"]
		else:
			print "Warning: %s was not recognized as a project directory"
	
	def __loadNetbeans(self, path):
		prop = JavaProperties(os.path.join(path, "nbproject", "project.properties"))
		self.name = prop.getEntry("application.title")
		possible = prop.getEntry("javac.classpath").split(":")
		exclude = [prop.getEntry("build.classes.dir"),]
		libfiles = []
		for lib in possible:
			libfile = os.path.join(path, lib)
			if os.path.exists(libfile):
				if os.path.isfile(libfile):
					libfiles.append(libfile)
				else:
					# what about dirs?
					pass
			else:
				print "Warning: %s does not exist" % libfile
		for libfile in libfiles:
			if "distributed" not in libfile:
				self.libs.append(libfile)

		self.main = os.path.join(path, prop.getEntry("dist.jar"))
		pass

	def __loadNbAndroid(self, path):
		prop = JavaProperties(os.path.join(path, "project.properties"))
		self.name = os.path.basename(os.path.dirname(path))
		self.main = os.path.join(path, "bin/classes.jar")
		lib = True
		i = 0
		while lib:
			i += 1
			lib = prop.getEntry("android.library.reference.%d" % i)
			if lib:
				if "distributed" in lib:
					continue
				lib = os.path.join(path,lib)
				if lib.endswith(".jar"):
					self.libs.append(lib)
				else:
					self.libs.append(JavaProject(lib).main)
				# self.__isNbAndroid(lib)
		

	def __isNetbeans(self, path):
		return os.path.exists(os.path.join(path, "nbproject", "project.properties"))

	def __isNbAndroid(self, path):
		nbandroidfolder = os.path.exists(os.path.join(path, "nbandroid"))
		if nbandroidfolder:
			return True
		else:
			return os.path.exists(os.path.join(path, "AndroidManifest.xml"))

	def ant(self, debug):
		targets = []
		if debug:
			targets.append(self.targets[0])
		else:
			targets.append(self.targets[1])
		ant(self.path, targets)
	
	def ant_clean(self):
		ant(self.path, ["clean",])

def mkcdb(cdbfile, serverproject, clientproject, useant=True, debug=False, clean=False):

	client_proj = JavaProject(clientproject)
	if useant:
		if clean:
			client_proj.ant_clean()
		client_proj.ant(debug)
	
	server_proj = JavaProject(serverproject)
	if useant:
		if clean:
			server_proj.ant_clean()
		server_proj.ant(debug)
	
	properties = {}
	prop = "name=%s\n" % os.path.splitext(os.path.basename(cdbfile))[0]

	# generate dex file
	handle, path = tempfile.mkstemp(suffix=".jar")
	libs = [client_proj.main,]
	libs.extend(client_proj.libs)
	mkdex(path, libs)

	zf = zipfile.ZipFile(cdbfile, mode="w")

	# add dex to zip
	print "Compressing %s.jar: %s" % (client_proj.name, os.path.relpath(path))
	zf.write(path, "%s.jar" % client_proj.name)
	properties["droid.binary"] = "%s.jar" % client_proj.name

	# add main jar
	print "Compressing %s.jar: %s" % (server_proj.name, os.path.relpath(server_proj.main))
	zf.write(server_proj.main, "%s.jar" % server_proj.name)
	properties["server.binary"] = "%s.jar" % server_proj.name

	i = 0
	for lib in server_proj.libs:
		print "Compressing lib %d.jar: %s" % (i,os.path.relpath(lib))
		zf.write(lib, "lib%d.jar" % i)
		properties["server.lib.%d" % i] = "lib%d.jar" % i
		i += 1
	
	#create
	keys = properties.keys()
	keys.sort()
	for key in keys:
		prop += "%s=%s\n" % (key, properties[key])
	
	zf.writestr("config.properties", prop)
	os.unlink(path)
	zf.close()


if __name__ == "__main__":
	parser = argparse.ArgumentParser(description="Create cdb[Candis Distributed Bundle]-File with Java-Project files")
	parser.add_argument("--noant", action="store_true")
	parser.add_argument("-d", "--debug", action="store_true")
	parser.add_argument("-c", "--clean", action="store_true")
	parser.add_argument("CDB", help="Path of the new cdb-file")
	parser.add_argument("CONTROL", help="Path to the control project")
	parser.add_argument("ANDROID", help="Path to the android project")
	args = parser.parse_args()
	mkcdb(args.CDB, args.CONTROL, args.ANDROID, not args.noant, args.debug, args.clean)
