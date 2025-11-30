-- Universal Loader for Fisch, Plants vs Brainrots, and any Roblox game
-- Works with file support, auto-updates, and clean UI

local LoaderVersion = "1.4"

if game.PlaceId == 16732661292 then
    game:GetService("StarterGui"):SetCore("SendNotification", {Title = "Loader", Text = "Detected: Fisch", Duration = 5})
elseif game.PlaceId == 18952823482 then -- Plants vs Brainrots (example ID, update if changed)
    game:GetService("StarterGui"):SetCore("SendNotification", {Title = "Loader", Text = "Detected: Plants vs Brainrots", Duration = 5})
end

local Scripts = {
    Fisch = "https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/new%20fisch",
    PvB = "https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/nbewpvb",
    Universal = "https://raw.githubusercontent.com/slowhub235/vhubV1/refs/heads/main/newbb"
}

local function LoadScript(url)
    if url then
        loadstring(game:HttpGet(url))()
    end
end

-- Auto-load best script based on game
if game.PlaceId == 16732661292 then -- Fisch
    LoadScript(Scripts.Fisch)
elseif game.PlaceId == 18952823482 or game.PlaceId == 19018400438 then -- Plants vs Brainrots (multiple IDs)
    LoadScript(Scripts.PvB)
else
    LoadScript(Scripts.Universal) -- fallback universal features
end
