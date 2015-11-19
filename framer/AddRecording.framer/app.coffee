# This imports all the layers for "title" into titleLayers
titleLayers = Framer.Importer.load "imported/title"
titleLayers.Layer_1.visible = false

titleLayers.Title.width = 460
titleLayers.Title.height = 280
titleLayers.Title.scale = .5
titleLayers.Title.x = -70
titleLayers.Title.y = -25
titleLayers.Title.opacity = 1

#First screen
layer = new BackgroundLayer 
	visible : true
	image: "images/background4.jpg"
	opacity: 1
	
white_cover = new Layer
	x : 20 #20
	y : 200 #200
	width : 280 #280
	height: 130 #130
	borderRadius: 20
	backgroundColor: "#ffffff"
	opacity: .65
	
# title = new Layer
# 	width: 310
# 	height: 300
# 	backgroundColor: null
# 	superLayer: layer
# 	color : "#000000"
# title.html ="<h2 style='padding-top:15px;text-align:center;'>Soundscape</h2>"

list_bullet = new Layer
	width : 50
	height: 50
	scale: .8
	image : "images/orangeplus.png"
	x: 25
	y: 200
	
list_bullet_2 = new Layer
	width : 50
	height: 50
	scale: .8
	image : "images/soundicon.png"
	x: 25
	y: 275
	
recording_start = new Layer
	x : 75
	y : 192
	width: 220
	height: 70
	backgroundColor: null
	color : "#000000"
	
recording_start.style.fontFamily = "Raleway"
recording_start.style.fontWeight = "500"
	
recording_start.html = "<p style='text-align: center; padding-top: 20px;'> Add a recording </p>"
	
recording_start_2 = new Layer
	x : 75
	y : 265
	width: 220
	height: 70
	backgroundColor: null
	color : "#000000"
	
recording_start_2.style.fontFamily = "Raleway"
recording_start_2.style.fontWeight = "500"
	
recording_start_2.html = "<p style='text-align: center; padding-top: 20px;'> Find a recording </p>"

recording_start.on Events.Click, ->
	record_screen.visible = true
	recording_start.visible = false
	recording_start_2.visible = false
	layer.visible = false
	list_bullet.visible = false
	list_bullet_2.visible = false
	titleLayers.Title.visible = false
	white_cover.visible = false


#Second screen
record_screen = new Layer
	width: 310
	height: 400
	image: "images/background4.jpg"
	opacity: .9	
	visible: false
	
	
soundgif = new Layer
	width: 310
	height: 100
	y: 50
	image: "images/giphy-still.png"
	superLayer: record_screen
	
record_screen_title = new Layer
	width: 220
	height: 50
	x : 53
	y : 170
	backgroundColor: null
	superLayer : record_screen
	color: "#000000"

record_screen_title.html = "<p> Start Recording! </p>"

record_button = new Layer
	width: 100
	height: 100
	y : 220
	image: "images/record.png"
	superLayer: record_screen
	
record_button.centerX()

Start = new Layer
	width: 220
	height: 60
	y : 230
	backgroundColor: "#80c3ec"
	borderRadius: 10
	visible : false
	color : "#000000"
Start.html = "<p style='text-align:center; padding-top: 12px; font-weight: 400;'>Back to start</p>"
Start.centerX()
	
Save = new Layer
	width: 220
	height: 60
	y : 300
	x : 160
	borderRadius: 10
	color : "#000000"
	backgroundColor: "#80c3ec"
	visible : false
Save.html = "<p style='text-align:center; padding-top: 12px; color: '#000000'; '>Save</p>"
Save.centerX()

record = true
finish = true

record_button.on Events.Click, ->
	if record == true and finish == true
		record_screen_title.html = "<p> Stop Recording </p>"
		soundgif.image = "images/giphy.gif"
		record = false
		finish = true
		Save.visible = false
		Start.visible = false
		record_button.visible = true
	else if record == false and finish == true
		record_screen_title.html = "<p style='padding-left: 70px'> Redo? </p>"
		soundgif.image = "images/giphy-still.tiff"
		record = false
		finish = false
		Save.visible = true
		Start.visible = true
		record_button.visible = false
	else if record == false and finish == false
		record_screen_title.html = "<p> Start Recording </p>"
		soundgif.image = "images/giphy-still.tiff"
		record = true
		finish = true
		record_button.visible = true
		Save.visible = false
		Start.visible = false
		
Start.on Events.Click, ->
		record_screen_title.html = "<p> Start Recording! </p>"
		soundgif.image = "images/giphy-still.tiff"
		record = true
		finish = true
		record_button.visible = true
		Save.visible = false
		Start.visible = false

confirm_box = new Layer
	width: 300
	height: 300
	backgroundColor: null
confirm_box.center()
confirm_box.visible = false
	
confirm = new Layer
	width: 190
	height: 190
	borderRadius: 100
	superLayer: confirm_box
confirm.centerX()
confirm.image = "images/okay.png"

confirm_text = new Layer
	width: 250
	height: 100
	y : 200
	backgroundColor: null
	superLayer: confirm_box
confirm_text.centerX()
confirm_text.html = "<p>Your recording has been saved! Please check your phone.</p>"

Save.on Events.Click, ->
	confirm_box.visible = true
	record_screen.opacity = .2
	recording_start.visible = false
	titleLayers.Title.visible = false
	Start.visible = false
	Save.visible = false





		
		
		
		

		
		

		
	
	
	
	


	