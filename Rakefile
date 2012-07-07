# Rakefile to help with standard blog activities
# Inspired by https://github.com/Bilalh/bilalh.github.com/blob/source/Rakefile 

task :default => :local

desc "Build from source and start the jekyll server (if not running already)"
task :build do
	#grep for a jekyll process
	fileName = '/tmp/jekyll.pid'
	started = false
	if File.exists? fileName
		pid = Integer(File.read(fileName))
		begin
			#if jekyll is alread running, then we are done
			Process.getpgid( pid )
			puts "Jekyll already running..."
			started = true
		rescue Errno::ESRCH
			#NOOP - we can start jekyll because it doens't exist
		end
	end

	#if the process isn't started, start it	
	unless started
		puts "Starting jeykll server..." 
		#Store the pid of jeykll server
		File.open(fileName, 'w+') do |f|
			# Spawn a new process and run the rake command
			pid = Process.spawn("jekyll --server", :out => '/dev/null', :err => '/dev/null')
			f.puts pid
		end

		# Detach the spawned process
		Process.detach pid
	end
end

desc "Start jekyll and refreshes the web page in Firefox"
task :refresh => :build do
	%x[
		# Check if Firefox is running, if so refresh
		ps -xc|grep -sqi firefox && osascript <<'APPLESCRIPT'
			 tell app "Firefox" to activate
			 tell app "System Events"
					if UI elements enabled then
						 keystroke "r" using command down
						 -- Fails if System Preferences > Universal access > "Enable access for assistive devices" is not on 
					--else
						 --tell app "Firefox" to Get URL "JavaScript:window.location.reload();" inside window 1
						 -- Fails if Firefox is set to open URLs from external apps in new tabs.
					end if
			 end tell
		APPLESCRIPT 
	]
end


desc "Makes a new post - rake new <post title>"
task :new do
	throw "No title given" unless ARGV[1]
	title = ""
	ARGV[1..ARGV.length - 1].each { |v| title += " #{v}" }
	title.strip!
	now = Time.now
	path = "_posts/#{now.strftime('%F')}-#{title.downcase.gsub(/[\s\.]/, '-').gsub(/[^\w\d\-]/, '')}.md"
	
	File.open(path, "w") do |f|
		f.puts "---"
		f.puts "layout: post"
		f.puts "title: #{title}"
		f.puts "date: #{now.strftime('%F %T')}"
		f.puts "description: "
		f.puts "tags:"
		f.puts "---"
		f.puts ""
		f.puts "# {{ page.title }}"
		f.puts "#{now.strftime('%B %e, %Y')} - "
	end
	
	exec("vim +5 #{path}")
	exit
end
