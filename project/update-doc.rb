#!/usr/bin/env ruby

require 'fileutils'

CURDIR = File.absolute_path(File.dirname(__FILE__))
Dir.chdir(File.join(CURDIR, ".."))

system("sbt \";clean;doc\"")

FileUtils.rm_r "target/website" if File.exist? "target/website"
FileUtils.mkdir_p "target/website"

unless system("sbt doc")
  exit 1
end

Dir["**/api/index.html"].each do |path|
  dirname = File.dirname(path)    
  raise unless dirname =~ /^([^\/]+)/
  project_name = $1
  
  puts "cp -r #{dirname} target/website/#{project_name}"
  FileUtils.cp_r(dirname, "target/website/#{project_name}")
end

FileUtils.rm_r "/tmp/shifter-api" if File.exists? "/tmp/shifter-api"
FileUtils.rm_r "/tmp/shifter" if File.exists? "/tmp/shifter"
FileUtils.cp_r("target/website", "/tmp/shifter-api")

Dir.chdir("/tmp")
system("git clone git@github.com:alexandru/shifter.git")
Dir.chdir("/tmp/shifter")

VERSION = File.read("VERSION")
system("git checkout gh-pages")

FileUtils.rm_r("api/#{VERSION}") if File.exists? "api/#{VERSION}"
FileUtils.cp_r("/tmp/shifter-api", "api/#{VERSION}")
FileUtils.rm_r("api/current") if File.exists? "api/#{VERSION}"
FileUtils.cp_r("/tmp/shifter-api", "api/current")

system("git add .")
system("git commit -a -m \"Update for #{VERSION}\"")
system("git push origin gh-pages")


