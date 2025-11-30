local fischIds = {
    16732694052,         -- Official Fisch
    131716211654599      -- Your custom Fisch place
}

local plantsVsBrainrotsId = 18763623305  -- Plants vs Brainrots PlaceId

local isFisch = false
for _, id in ipairs(fischIds) do
    if game.PlaceId == id then
        isFisch = true
        break
    end
end

if isFisch then
    print("Fisch detected (PlaceId: " .. game.PlaceId .. ")! Loading fisch script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/new%20fisch"))()
    
elseif game.PlaceId == plantsVsBrainrotsId then
    print("Plants vs Brainrots detected (PlaceId: " .. game.PlaceId .. ")! Loading Plants vs Brainrots script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/nbewpvb"))()  -- change "pvb" to whatever your script file is named
    
else
    print("Not Fisch or Plants vs Brainrots! Loading default Blade Ball script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/newbb"))()
end

print("Loader complete!")
