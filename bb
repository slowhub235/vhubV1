local fischIds = {
    16732694052,         -- Official Fisch
    131716211654599      -- Your custom Fisch place
}

local mm2Ids = {
    142823291,    -- Murder Mystery 2 (main)
    6938051517,   -- MM2 Winter / Events
    1054526971,   -- MM2 old/testing
    -- add more MM2 place IDs if needed
}

local isFisch = false
local isMM2 = false

for _, id in ipairs(fischIds) do
    if game.PlaceId == id then
        isFisch = true
        break
    end
end

if not isFisch then
    for _, id in ipairs(mm2Ids) do
        if game.PlaceId == id then
            isMM2 = true
            break
        end
    end
end

if isFisch then
    print("Fisch detected (PlaceId: " .. game.PlaceId .. ")! Loading Fisch script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/new%20fisch"))()

elseif isMM2 then
    print("Murder Mystery 2 detected (PlaceId: " .. game.PlaceId .. ")! Loading MM2 script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/mm2"))()  -- ← put your MM2 script name here

else
    print("Not Fisch or MM2 → Loading Blade Ball script...")
    loadstring(game:HttpGet("https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/newbb"))()
end

print("Loader complete! (Fisch / MM2 / Blade Ball)")
