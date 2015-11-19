bg = new Layer
	width: 800
	height: 1350
	image: "images/background4.jpg"
	
title = new Layer
	width: 450
	height: 200
	y: 50
	backgroundColor: null
	color: "#000000"
	html: "<br/><h1>A Family Picnic</h1>"
	superLayer: bg
title.centerX()

desc = new Layer
	width: 700
	height: 200
	x: 50
	y: 150
	backgroundColor: null
	color: "#000000"
	html: "<br/><h3>Me and my wonderful family enjoying food in Golden Gate Park.</h3>"
	superLayer: bg

image = new Layer
	width: 520
	height: 360
	image: "images/picnic.jpg"
	x: 0
	y: 300
	superLayer: bg
	borderWidth: 5
	borderColor: "#000000"
image.centerX()

play = new Layer
	width: 250
	height: 250
	superLayer: bg
	backgroundColor: "#FFFFFF"
	image: "images/play2.png"
	y: 700
play.centerX()
isplay = true
play.on Events.Click, ->
	if isplay == true
		play.image = "images/pause.png"
		isplay = false
	else
		play.image = "images/play2.png"
		isplay = true
		
rate = new Layer
	width: 540
	height: 100
	image: "images/rating.png"
	x: 0
	y: 1000
	superLayer: bg
	borderWidth: 10
rate.centerX()

edit = new Layer
	width: 380
	height: 200
	superLayer: bg
	backgroundColor: "#6d9de9"
	opacity: .8
	borderWidth: 5
	borderColor: "#000000"
	x: -3
	y: 1150
edit.html = "<br/><br/><br/><h1>Edit</h1>"
edit.style["text-align"] = "center"
edit.style["color"] = "black"

edit.on Events.Click, ->
	bg.animate
		properties: 
			x: -800
		curve: "ease-in-out"
		time: .2
	eg.animate
		properties: 
			x: -20
		curve: "ease-in-out"
		time: .2
edit.on Events.MouseDown, ->
	edit.backgroundColor = "#6d9de9"
edit.on Events.MouseUp, ->
	edit.backgroundColor = "#6d9de9"
	
del = new Layer
	width: 380
	height: 200
	superLayer: bg
	backgroundColor: "#6d9de9"
	opacity: .8	
	borderWidth: 5
	borderColor: "#000000"
	x: 370
	y: 1150
del.html = "<br/><br/><br/><h1>Delete</h1>"
del.style["text-align"] = "center"
del.style["color"] = "black"

del.on Events.MouseDown, ->
	del.backgroundColor = "#6d9de9"
del.on Events.MouseUp, ->
	del.backgroundColor = "#6d9de9"

#edit screen
eg = new Layer
	width: 800
	height: 1350
	image: "images/background4.jpg"
	x: 780
	y: 0

edit_title = new Layer
	width: 650
	height: 400
	y: 50
	backgroundColor: null
	color: "#000000"
	html: "<br/><h3>Title:</h3><br/><input type='text' name='title' style='width:700px;height:60px;font-size:48px; padding-left : 5px;' value='A Family Picnic'>"
	superLayer: eg
edit_title.centerX()
edit_title.style["font-size"] = "48px"

edit_desc = new Layer
	width: 650
	height: 400
	y: 200
	backgroundColor: null
	color: "#000000"
	html: "<br/><h3>Description:</h3><br/><textarea type='text' name='title' style='width:650px;height:400px;font-size:48px; padding-left : 5px;'>Me and my wonderful family enjoying food in Golden Gate Park.</textarea>"
	superLayer: eg
edit_desc.centerX()
edit_desc.style["font-size"] = "48px"

edit_cat = new Layer
	width: 650
	height: 200
	y: 600
	backgroundColor: null
	color: "#000000"
	superLayer: eg
edit_cat.centerX()
edit_cat.style["font-size"] = "48px"
edit_cat.html = "<br/><h3>Category:<br/><br/><select style='width:700px;height:60px;font-size:36px'>
  <option value='personal'>Personal Moment</option>
  <option value='group'>Group Moment</option>
  <option value='info'>Informative</option>
  <option value='ambient'>Ambient</option>
  <option value='music'>Music</option>
</select>"

edit_pub = new Layer
	width: 650
	height: 400
	x: 70
	y: 800
	backgroundColor: null
	color: "#000000"
	superLayer: eg
	html: "<br /><h3>Publish</h3>"
edit_pub.style["font-size"] = "48px"

edit_pub_butt = new Layer
	width: 150
	height: 100
	superLayer: eg
	x: 70
	y: 900
	image: "images/on.png"
ispub = true
edit_pub_butt.on Events.Click, ->
	if ispub == true
		edit_pub_butt.image = "images/off.png"
		ispub = false
	else
		edit_pub_butt.image = "images/on.png"
		ispub = true

cancel = new Layer
	width: 400
	height: 200
	superLayer: eg
	backgroundColor: "#6d9de9"
	opacity: .8
	borderWidth: 5
	borderColor: "#000000"
	y: 1150
cancel.html = "<br/><br/><br/><h1>Cancel</h1>"
cancel.style["text-align"] = "center"
cancel.style["color"] = "black"

cancel.on Events.Click, ->
	bg.animate
		properties: 
			x: 0
		curve: "linear"
		time: .2
	eg.animate
		properties: 
			x: 800
		curve: "linear"
		time: .2
	
cancel.on Events.MouseDown, ->
	cancel.backgroundColor = "#6d9de9"
cancel.on Events.MouseUp, ->
	cancel.backgroundColor = "#6d9de9"
	
save = new Layer
	width: 400
	height: 200
	superLayer: eg
	backgroundColor: "#6d9de9"
	opacity: .8
	borderWidth: 5
	borderColor: "#000000"
	x: 395
	y: 1150
save.html = "<br/><br/><br/><h1>Save</h1>"
save.style["text-align"] = "center"
save.style["color"] = "black"

save.on Events.Click, ->
	bg.animate
		properties: 
			x: 0
		curve: "ease-in-out"
		time: .2
	eg.animate
		properties: 
			x: 800
		curve: "ease-in-out"
		time: .2
	
save.on Events.MouseDown, ->
	save.backgroundColor = "#6d9de9"
save.on Events.MouseUp, ->
	save.backgroundColor = "#6d9de9"