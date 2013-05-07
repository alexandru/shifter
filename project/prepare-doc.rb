#!/usr/bin/env ruby

require 'fileutils'

CURDIR = File.absolute_path(File.dirname(__FILE__))
Dir.chdir(File.join(CURDIR, ".."))

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


