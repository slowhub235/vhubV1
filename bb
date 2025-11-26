
getgenv().GG = {
    Language = {
        CheckboxEnabled = "Enabled",
        CheckboxDisabled = "Disabled",
        SliderValue = "Value",
        DropdownSelect = "Select",
        DropdownNone = "None",
        DropdownSelected = "Selected",
        ButtonClick = "Click",
        TextboxEnter = "Enter",
        ModuleEnabled = "Enabled",
        ModuleDisabled = "Disabled",
        TabGeneral = "General",
        TabSettings = "Settings",
        Loading = "Loading...",
        Error = "Error",
        Success = "Success"
    }
}

local SelectedLanguage = GG.Language

function convertStringToTable(inputString)
    local result = {}
    for value in string.gmatch(inputString, "([^,]+)") do
        local trimmedValue = value:match("^%s*(.-)%s*$")
        table.insert(result, trimmedValue)
    end

    return result
end

function convertTableToString(inputTable)
    return table.concat(inputTable, ", ")
end

local UserInputService = cloneref(game:GetService('UserInputService'))
local ContentProvider = cloneref(game:GetService('ContentProvider'))
local TweenService = cloneref(game:GetService('TweenService'))
local HttpService = cloneref(game:GetService('HttpService'))
local TextService = cloneref(game:GetService('TextService'))
local RunService = cloneref(game:GetService('RunService'))
local Lighting = cloneref(game:GetService('Lighting'))
local Players = cloneref(game:GetService('Players'))
local CoreGui = cloneref(game:GetService('CoreGui'))
local Debris = cloneref(game:GetService('Debris'))

local mouse = Players.LocalPlayer:GetMouse()
local old_Skuwu = CoreGui:FindFirstChild('Skuwu')

if old_Skuwu then
    Debris:AddItem(old_Skuwu, 0)
end

if not isfolder("Skuwu") then
    makefolder("Skuwu")
end


local Connections = setmetatable({
    disconnect = function(self, connection)
        if not self[connection] then
            return
        end
    
        self[connection]:Disconnect()
        self[connection] = nil
    end,
    disconnect_all = function(self)
        for _, value in self do
            if typeof(value) == 'function' then
                continue
            end
    
            value:Disconnect()
        end
    end
}, Connections)


local Util = setmetatable({
    map = function(self, value, in_minimum, in_maximum, out_minimum, out_maximum)
        return (value - in_minimum) * (out_maximum - out_minimum) / (in_maximum - in_minimum) + out_minimum
    end,
    viewport_point_to_world = function(self, location, distance)
        local unit_ray = workspace.CurrentCamera:ScreenPointToRay(location.X, location.Y)

        return unit_ray.Origin + unit_ray.Direction * distance
    end,
    get_offset = function(self)
        local viewport_size_Y = workspace.CurrentCamera.ViewportSize.Y

        return self:map(viewport_size_Y, 0, 2560, 8, 56)
    end
}, Util)


local AcrylicBlur = {}
AcrylicBlur.__index = AcrylicBlur


function AcrylicBlur.new(object)
    local self = setmetatable({
        _object = object,
        _folder = nil,
        _frame = nil,
        _root = nil
    }, AcrylicBlur)

    self:setup()

    return self
end


function AcrylicBlur:create_folder()
    local old_folder = workspace.CurrentCamera:FindFirstChild('AcrylicBlur')

    if old_folder then
        Debris:AddItem(old_folder, 0)
    end

    local folder = Instance.new('Folder')
    folder.Name = 'AcrylicBlur'
    folder.Parent = workspace.CurrentCamera

    self._folder = folder
end


function AcrylicBlur:create_depth_of_fields()
    local depth_of_fields = Lighting:FindFirstChild('AcrylicBlur') or Instance.new('DepthOfFieldEffect')
    depth_of_fields.FarIntensity = 0
    depth_of_fields.FocusDistance = 0.05
    depth_of_fields.InFocusRadius = 0.1
    depth_of_fields.NearIntensity = 1
    depth_of_fields.Name = 'AcrylicBlur'
    depth_of_fields.Parent = Lighting

    for _, object in Lighting:GetChildren() do
        if not object:IsA('DepthOfFieldEffect') then
            continue
        end

        if object == depth_of_fields then
            continue
        end

        Connections[object] = object:GetPropertyChangedSignal('FarIntensity'):Connect(function()
            object.FarIntensity = 0
        end)

        object.FarIntensity = 0
    end
end


function AcrylicBlur:create_frame()
    local frame = Instance.new('Frame')
    frame.Size = UDim2.new(1, 0, 1, 0)
    frame.Position = UDim2.new(0.5, 0, 0.5, 0)
    frame.AnchorPoint = Vector2.new(0.5, 0.5)
    frame.BackgroundTransparency = 1
    frame.Parent = self._object

    self._frame = frame
end


function AcrylicBlur:create_root()
    local part = Instance.new('Part')
    part.Name = 'Root'
    part.Color = Color3.new(0, 0, 0)
    part.Material = Enum.Material.Glass
    part.Size = Vector3.new(1, 1, 0)
    part.Anchored = true
    part.CanCollide = false
    part.CanQuery = false
    part.Locked = true
    part.CastShadow = false
    part.Transparency = 0.98
    part.Parent = self._folder

    local specialMesh = Instance.new('SpecialMesh')
    specialMesh.MeshType = Enum.MeshType.Brick
    specialMesh.Offset = Vector3.new(0, 0, -0.000001)
    specialMesh.Parent = part

    self._root = part
end


function AcrylicBlur:setup()
    self:create_depth_of_fields()
    self:create_folder()
    self:create_root()
    
    self:create_frame()
    self:render(0.001)

    self:check_quality_level()
end


function AcrylicBlur:render(distance)
    local positions = {
        top_left = Vector2.new(),
        top_right = Vector2.new(),
        bottom_right = Vector2.new(),
    }

    local function update_positions(size, position)
        positions.top_left = position
        positions.top_right = position + Vector2.new(size.X, 0)
        positions.bottom_right = position + size
    end

    local function update()
        local top_left = positions.top_left
        local top_right = positions.top_right
        local bottom_right = positions.bottom_right

        local top_left3D = Util:viewport_point_to_world(top_left, distance)
        local top_right3D = Util:viewport_point_to_world(top_right, distance)
        local bottom_right3D = Util:viewport_point_to_world(bottom_right, distance)

        local width = (top_right3D - top_left3D).Magnitude
        local height = (top_right3D - bottom_right3D).Magnitude

        if not self._root then
            return
        end

        self._root.CFrame = CFrame.fromMatrix((top_left3D + bottom_right3D) / 2, workspace.CurrentCamera.CFrame.XVector, workspace.CurrentCamera.CFrame.YVector, workspace.CurrentCamera.CFrame.ZVector)
        self._root.Mesh.Scale = Vector3.new(width, height, 0)
    end

    local function on_change()
        local offset = Util:get_offset()
        local size = self._frame.AbsoluteSize - Vector2.new(offset, offset)
        local position = self._frame.AbsolutePosition + Vector2.new(offset / 2, offset / 2)

        update_positions(size, position)
        task.spawn(update)
    end

    Connections['cframe_update'] = workspace.CurrentCamera:GetPropertyChangedSignal('CFrame'):Connect(update)
    Connections['viewport_size_update'] = workspace.CurrentCamera:GetPropertyChangedSignal('ViewportSize'):Connect(update)
    Connections['field_of_view_update'] = workspace.CurrentCamera:GetPropertyChangedSignal('FieldOfView'):Connect(update)

    Connections['frame_absolute_position'] = self._frame:GetPropertyChangedSignal('AbsolutePosition'):Connect(on_change)
    Connections['frame_absolute_size'] = self._frame:GetPropertyChangedSignal('AbsoluteSize'):Connect(on_change)
    
    task.spawn(update)
end


function AcrylicBlur:check_quality_level()
    local game_settings = UserSettings().GameSettings
    local quality_level = game_settings.SavedQualityLevel.Value

    if quality_level < 8 then
        self:change_visiblity(false)
    end

    Connections['quality_level'] = game_settings:GetPropertyChangedSignal('SavedQualityLevel'):Connect(function()
        local game_settings = UserSettings().GameSettings
        local quality_level = game_settings.SavedQualityLevel.Value

        self:change_visiblity(quality_level >= 8)
    end)
end


function AcrylicBlur:change_visiblity(state)
    self._root.Transparency = state and 0.98 or 1
end


local Config = setmetatable({
    save = function(self, file_name, config)
        local success_save, result = pcall(function()
            local flags = HttpService:JSONEncode(config)
            writefile('Skuwu/'..file_name..'.json', flags)
        end)
    
        if not success_save then
            warn('failed to save config', result)
        end
    end,
    load = function(self, file_name, config)
        local success_load, result = pcall(function()
            if not isfile('Skuwu/'..file_name..'.json') then
                self:save(file_name, config)
        
                return
            end
        
            local flags = readfile('Skuwu/'..file_name..'.json')
        
            if not flags then
                self:save(file_name, config)
        
                return
            end

            return HttpService:JSONDecode(flags)
        end)
    
        if not success_load then
            warn('failed to load config', result)
        end
    
        if not result then
            result = {
                _flags = {},
                _keybinds = {},
                _library = {}
            }
        end
    
        return result
    end
}, Config)


local Library = {
    _config = Config:load(game.GameId),

    _choosing_keybind = false,
    _device = nil,

    _ui_open = true,
    _ui_scale = 1,
    _ui_loaded = false,
    _ui = nil,

    _dragging = false,
    _drag_start = nil,
    _container_position = nil
}
Library.__index = Library


function Library.new()
    local self = setmetatable({
        _loaded = false,
        _tab = 0,
    }, Library)
    
    self:create_ui()

    return self
end

local NotificationContainer = Instance.new("Frame")
NotificationContainer.Name = "RobloxCoreGuis"
NotificationContainer.Size = UDim2.new(0, 300, 0, 0)
NotificationContainer.Position = UDim2.new(0.8, 0, 0, 10)
NotificationContainer.BackgroundTransparency = 1
NotificationContainer.ClipsDescendants = false
NotificationContainer.Parent = game:GetService("CoreGui").RobloxGui:FindFirstChild("RobloxCoreGuis") or Instance.new("ScreenGui", game:GetService("CoreGui").RobloxGui)
NotificationContainer.AutomaticSize = Enum.AutomaticSize.Y

local UIListLayout = Instance.new("UIListLayout")
UIListLayout.FillDirection = Enum.FillDirection.Vertical
UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
UIListLayout.Padding = UDim.new(0, 10)
UIListLayout.Parent = NotificationContainer

function Library.SendNotification(settings)
    local Notification = Instance.new("Frame")
    Notification.Size = UDim2.new(1, 0, 0, 60)
    Notification.BackgroundTransparency = 1
    Notification.BorderSizePixel = 0
    Notification.Name = "Notification"
    Notification.Parent = NotificationContainer
    Notification.AutomaticSize = Enum.AutomaticSize.Y

    local UICorner = Instance.new("UICorner")
    UICorner.CornerRadius = UDim.new(0, 4)
    UICorner.Parent = Notification

    local InnerFrame = Instance.new("Frame")
    InnerFrame.Size = UDim2.new(1, 0, 0, 60)
    InnerFrame.Position = UDim2.new(0, 0, 0, 0)
    InnerFrame.BackgroundColor3 = Color3.fromRGB(0,0,0)
    InnerFrame.BackgroundTransparency = 0.1
    InnerFrame.BorderSizePixel = 0
    InnerFrame.Name = "InnerFrame"
    InnerFrame.Parent = Notification
    InnerFrame.AutomaticSize = Enum.AutomaticSize.Y

    local InnerUICorner = Instance.new("UICorner")
    InnerUICorner.CornerRadius = UDim.new(0, 4)
    InnerUICorner.Parent = InnerFrame

    local Title = Instance.new("TextLabel")
    Title.Text = settings.title or "Notification Title"
    Title.TextColor3 = Color3.fromRGB(255,255,255)
    Title.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
    Title.TextSize = 14
    Title.Size = UDim2.new(1, -10, 0, 20)
    Title.Position = UDim2.new(0, 5, 0, 5)
    Title.BackgroundTransparency = 1
    Title.TextXAlignment = Enum.TextXAlignment.Left
    Title.TextYAlignment = Enum.TextYAlignment.Center
    Title.TextWrapped = true
    Title.AutomaticSize = Enum.AutomaticSize.Y
    Title.Parent = InnerFrame

    local Body = Instance.new("TextLabel")
    Body.Text = settings.text or "This is the body of the notification."
    Body.TextColor3 = Color3.fromRGB(180, 180, 180)
    Body.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
    Body.TextSize = 12
    Body.Size = UDim2.new(1, -10, 0, 30)
    Body.Position = UDim2.new(0, 5, 0, 25)
    Body.BackgroundTransparency = 1
    Body.TextXAlignment = Enum.TextXAlignment.Left
    Body.TextYAlignment = Enum.TextYAlignment.Top
    Body.TextWrapped = true
    Body.AutomaticSize = Enum.AutomaticSize.Y
    Body.Parent = InnerFrame

    task.spawn(function()
        wait(0.1)
        local totalHeight = Title.TextBounds.Y + Body.TextBounds.Y + 10
        InnerFrame.Size = UDim2.new(1, 0, 0, totalHeight)
    end)

    task.spawn(function()
        local tweenIn = TweenService:Create(InnerFrame, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
            Position = UDim2.new(0, 0, 0, 10 + NotificationContainer.Size.Y.Offset)
        })
        tweenIn:Play()

        local duration = settings.duration or 5
        wait(duration)

        local tweenOut = TweenService:Create(InnerFrame, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.In), {
            Position = UDim2.new(1, 310, 0, 10 + NotificationContainer.Size.Y.Offset)
        })
        tweenOut:Play()

        tweenOut.Completed:Connect(function()
            Notification:Destroy()
        end)
    end)
end

function Library:get_screen_scale()
    local viewport_size_x = workspace.CurrentCamera.ViewportSize.X

    self._ui_scale = viewport_size_x / 1400
end


function Library:get_device()
    local device = 'Unknown'

    if not UserInputService.TouchEnabled and UserInputService.KeyboardEnabled and UserInputService.MouseEnabled then
        device = 'PC'
    elseif UserInputService.TouchEnabled then
        device = 'Mobile'
    elseif UserInputService.GamepadEnabled then
        device = 'Console'
    end

    self._device = device
end


function Library:removed(action)
    self._ui.AncestryChanged:Once(action)
end


function Library:flag_type(flag, flag_type)
    if not Library._config._flags[flag] then
        return
    end

    return typeof(Library._config._flags[flag]) == flag_type
end


function Library:remove_table_value(__table, table_value)
    for index, value in __table do
        if value ~= table_value then
            continue
        end

        table.remove(__table, index)
    end
end


function Library:create_ui()
    local old_Skuwu = CoreGui:FindFirstChild('Skuwu')

    if old_Skuwu then
        Debris:AddItem(old_Skuwu, 0)
    end

    local Skuwu = Instance.new('ScreenGui')
    Skuwu.ResetOnSpawn = false
    Skuwu.Name = 'Skuwu'
    Skuwu.ZIndexBehavior = Enum.ZIndexBehavior.Sibling
    Skuwu.Parent = CoreGui
    
    local Container = Instance.new('Frame')
    Container.ClipsDescendants = true
    Container.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Container.AnchorPoint = Vector2.new(0.5, 0.5)
    Container.Name = 'Container'
    Container.BackgroundTransparency = 0.05000000074505806
    Container.BackgroundColor3 = Color3.fromRGB(0,0,0)
    Container.Position = UDim2.new(0.5, 0, 0.5, 0)
    Container.Size = UDim2.new(0, 0, 0, 0)
    Container.Active = true
    Container.BorderSizePixel = 0
    Container.Parent = Skuwu
    
    local UICorner = Instance.new('UICorner')
    UICorner.CornerRadius = UDim.new(0, 10)
    UICorner.Parent = Container
    
    local UIStroke = Instance.new('UIStroke')
    UIStroke.Color = Color3.fromRGB(255,255,255)
    UIStroke.Transparency = 0.5
    UIStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
    UIStroke.Parent = Container
    
    local Handler = Instance.new('Frame')
    Handler.BackgroundTransparency = 1
    Handler.Name = 'Handler'
    Handler.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Handler.Size = UDim2.new(0, 698, 0, 479)
    Handler.BorderSizePixel = 0
    Handler.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Handler.Parent = Container
    
    local Tabs = Instance.new('ScrollingFrame')
    Tabs.ScrollBarImageTransparency = 1
    Tabs.ScrollBarThickness = 0
    Tabs.Name = 'Tabs'
    Tabs.Size = UDim2.new(0, 129, 0, 401)
    Tabs.Selectable = false
    Tabs.AutomaticCanvasSize = Enum.AutomaticSize.XY
    Tabs.BackgroundTransparency = 1
    Tabs.Position = UDim2.new(0.026097271591424942, 0, 0.1111111119389534, 0)
    Tabs.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Tabs.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Tabs.BorderSizePixel = 0
    Tabs.CanvasSize = UDim2.new(0, 0, 0.5, 0)
    Tabs.Parent = Handler
    
    local UIListLayout = Instance.new('UIListLayout')
    UIListLayout.Padding = UDim.new(0, 4)
    UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
    UIListLayout.Parent = Tabs
    
    local ClientName = Instance.new('TextLabel')
ClientName.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
ClientName.TextColor3 = Color3.fromRGB(255,255,255)
ClientName.TextTransparency = 0.2
ClientName.Text = '     hub  '
ClientName.Name = 'ClientName'
ClientName.Size = UDim2.new(0, 31, 0, 13)
ClientName.AnchorPoint = Vector2.new(0, 0.5)
ClientName.Position = UDim2.new(0.036, 0, 0.055, 0) -- moved slightly left
ClientName.BackgroundTransparency = 1
ClientName.TextXAlignment = Enum.TextXAlignment.Left
ClientName.BorderSizePixel = 0
ClientName.BorderColor3 = Color3.fromRGB(50, 50, 50)
ClientName.TextSize = 13
ClientName.BackgroundColor3 = Color3.fromRGB(255,255,255)
ClientName.Parent = Handler

local BetaLabel = Instance.new('TextLabel')
BetaLabel.Font = Enum.Font.FredokaOne -- cartoony font
BetaLabel.TextColor3 = Color3.fromRGB(0, 170, 255) -- bright blue
BetaLabel.Text = 'BETA'
BetaLabel.Name = 'BetaLabel'
BetaLabel.Size = UDim2.new(0, 50, 0, 18) -- bigger
BetaLabel.AnchorPoint = Vector2.new(0, 0.5)
BetaLabel.Position = UDim2.new(0.11, 0, 0.055, 0) -- slightly left for better fit
BetaLabel.BackgroundTransparency = 1
BetaLabel.TextXAlignment = Enum.TextXAlignment.Left
BetaLabel.TextSize = 18 -- bigger
BetaLabel.Parent = Handler
    
    local UIGradient = Instance.new('UIGradient')
    UIGradient.Color = ColorSequence.new{
        ColorSequenceKeypoint.new(0, Color3.fromRGB(155, 155, 155)),
        ColorSequenceKeypoint.new(1, Color3.fromRGB(255,255,255))
    }
    UIGradient.Parent = ClientName
    
    local Pin = Instance.new('Frame')
    Pin.Name = 'Pin'
    Pin.Position = UDim2.new(0.026000000536441803, 0, 0.13600000739097595, 0)
    Pin.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Pin.Size = UDim2.new(0, 2, 0, 16)
    Pin.BorderSizePixel = 0
    Pin.BackgroundTransparency = 1
    Pin.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Pin.Parent = Handler
    
    local UICorner = Instance.new('UICorner')
    UICorner.CornerRadius = UDim.new(1, 0)
    UICorner.Parent = Pin
    
    local Icon = Instance.new('ImageLabel')
    Icon.ImageColor3 = Color3.fromRGB(255,255,255)
    Icon.ScaleType = Enum.ScaleType.Fit
    Icon.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Icon.AnchorPoint = Vector2.new(0, 0.5)
    Icon.Image = 'rbxassetid://9400168569'
    Icon.BackgroundTransparency = 1
    Icon.Position = UDim2.new(0.02500000037252903, 0, 0.054999999701976776, 0)
    Icon.Name = 'Icon'
    Icon.Size = UDim2.new(0, 25, 0 , 23)
    Icon.BorderSizePixel = 0
    Icon.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Icon.Parent = Handler
    
    local Divider = Instance.new('Frame')
    Divider.Name = 'Divider'
    Divider.BackgroundTransparency = 0.5
    Divider.Position = UDim2.new(0.23499999940395355, 0, 0, 0)
    Divider.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Divider.Size = UDim2.new(0, 1, 0, 479)
    Divider.BorderSizePixel = 0
    Divider.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Divider.Parent = Handler
    
    local Sections = Instance.new('Folder')
    Sections.Name = 'Sections'
    Sections.Parent = Handler
    
    local Minimize = Instance.new('TextButton')
    Minimize.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
    Minimize.TextColor3 = Color3.fromRGB(50, 50, 50)
    Minimize.BorderColor3 = Color3.fromRGB(50, 50, 50)
    Minimize.Text = ''
    Minimize.AutoButtonColor = false
    Minimize.Name = 'Minimize'
    Minimize.BackgroundTransparency = 1
    Minimize.Position = UDim2.new(0.020057305693626404, 0, 0.02922755666077137, 0)
    Minimize.Size = UDim2.new(0, 24, 0, 24)
    Minimize.BorderSizePixel = 0
    Minimize.TextSize = 14
    Minimize.BackgroundColor3 = Color3.fromRGB(255,255,255)
    Minimize.Parent = Handler
    
    local UIScale = Instance.new('UIScale')
    UIScale.Parent = Container    
    
    self._ui = Skuwu

    local function on_drag(input, process)
        if input.UserInputType == Enum.UserInputType.MouseButton1 or input.UserInputType == Enum.UserInputType.Touch then 
            self._dragging = true
            self._drag_start = input.Position
            self._container_position = Container.Position

            Connections['container_input_ended'] = input.Changed:Connect(function()
                if input.UserInputState ~= Enum.UserInputState.End then
                    return
                end

                Connections:disconnect('container_input_ended')
                self._dragging = false
            end)
        end
    end

    local function update_drag(input)
        local delta = input.Position - self._drag_start
        local position = UDim2.new(self._container_position.X.Scale, self._container_position.X.Offset + delta.X, self._container_position.Y.Scale, self._container_position.Y.Offset + delta.Y)

        TweenService:Create(Container, TweenInfo.new(0.2), {
            Position = position
        }):Play()
    end

    local function drag(input, process)
        if not self._dragging then
            return
        end

        if input.UserInputType == Enum.UserInputType.MouseMovement or input.UserInputType == Enum.UserInputType.Touch then
            update_drag(input)
        end
    end

    Connections['container_input_began'] = Container.InputBegan:Connect(on_drag)
    Connections['input_changed'] = UserInputService.InputChanged:Connect(drag)

    self:removed(function()
        self._ui = nil
        Connections:disconnect_all()
    end)

    function self:Update1Run(a)
        if a == "nil" then
            Container.BackgroundTransparency = 0.05000000074505806
        else
            pcall(function()
                Container.BackgroundTransparency = tonumber(a)
            end)
        end
    end

    function self:UIVisiblity()
        Skuwu.Enabled = not Skuwu.Enabled
    end

    function self:change_visiblity(state)
        if state then
            TweenService:Create(Container, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                Size = UDim2.fromOffset(698, 479)
            }):Play()
        else
            TweenService:Create(Container, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                Size = UDim2.fromOffset(130, 52)
            }):Play()
        end
    end
    

    function self:load()
        local content = {}
    
        for _, object in Skuwu:GetDescendants() do
            if not object:IsA('ImageLabel') then
                continue
            end
    
            table.insert(content, object)
        end
    
        ContentProvider:PreloadAsync(content)
        self:get_device()

        if self._device == 'Mobile' or self._device == 'Unknown' then
            self:get_screen_scale()
            UIScale.Scale = self._ui_scale
    
            Connections['ui_scale'] = workspace.CurrentCamera:GetPropertyChangedSignal('ViewportSize'):Connect(function()
                self:get_screen_scale()
                UIScale.Scale = self._ui_scale
            end)
        end
    
        TweenService:Create(Container, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
            Size = UDim2.fromOffset(698, 479)
        }):Play()

        AcrylicBlur.new(Container)
        self._ui_loaded = true
    end

    function self:update_tabs(tab)
        for index, object in Tabs:GetChildren() do
            if object.Name ~= 'Tab' then
                continue
            end

            if object == tab then
                if object.BackgroundTransparency ~= 0.5 then
                    local offset = object.LayoutOrder * (0.113 / 1.3)

                    TweenService:Create(Pin, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        Position = UDim2.fromScale(0.026, 0.135 + offset)
                    }):Play()    

                    TweenService:Create(object, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundTransparency = 0.5
                    }):Play()

                    TweenService:Create(object.TextLabel, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        TextTransparency = 0.2,
                        TextColor3 = Color3.fromRGB(255,255,255)
                    }):Play()

                    TweenService:Create(object.TextLabel.UIGradient, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        Offset = Vector2.new(1, 0)
                    }):Play()

                    TweenService:Create(object.Icon, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        ImageTransparency = 0.2,
                        ImageColor3 = Color3.fromRGB(255,255,255)
                    }):Play()
                end

                continue
            end

            if object.BackgroundTransparency ~= 1 then
                TweenService:Create(object, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                    BackgroundTransparency = 1
                }):Play()
                
                TweenService:Create(object.TextLabel, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                    TextTransparency = 0.7,
                    TextColor3 = Color3.fromRGB(255,255,255)
                }):Play()

                TweenService:Create(object.TextLabel.UIGradient, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                    Offset = Vector2.new(0, 0)
                }):Play()

                TweenService:Create(object.Icon, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                    ImageTransparency = 0.8,
                    ImageColor3 = Color3.fromRGB(255,255,255)
                }):Play()
            end
        end
    end

    function self:update_sections(left_section, right_section)
        for _, object in Sections:GetChildren() do
            if object == left_section or object == right_section then
                object.Visible = true

                continue
            end

            object.Visible = false
        end
    end

    function self:create_tab(title, icon)
        local TabManager = {}

        local LayoutOrder = 0

        local font_params = Instance.new('GetTextBoundsParams')
        font_params.Text = title
        font_params.Font = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
        font_params.Size = 13
        font_params.Width = 10000

        local font_size = TextService:GetTextBoundsAsync(font_params)
        local first_tab = not Tabs:FindFirstChild('Tab')

        local Tab = Instance.new('TextButton')
        Tab.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
        Tab.TextColor3 = Color3.fromRGB(50, 50, 50)
        Tab.BorderColor3 = Color3.fromRGB(50, 50, 50)
        Tab.Text = ''
        Tab.AutoButtonColor = false
        Tab.BackgroundTransparency = 1
        Tab.Name = 'Tab'
        Tab.Size = UDim2.new(0, 129, 0, 38)
        Tab.BorderSizePixel = 0
        Tab.TextSize = 14
        Tab.BackgroundColor3 = Color3.fromRGB(0,0,0)
        Tab.Parent = Tabs
        Tab.LayoutOrder = self._tab
        
        local UICorner = Instance.new('UICorner')
        UICorner.CornerRadius = UDim.new(0, 5)
        UICorner.Parent = Tab
        
        local TextLabel = Instance.new('TextLabel')
        TextLabel.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
        TextLabel.TextColor3 = Color3.fromRGB(255,255,255)
        TextLabel.TextTransparency = 0.7
        TextLabel.Text = title
        TextLabel.Size = UDim2.new(0, font_size.X, 0, 16)
        TextLabel.AnchorPoint = Vector2.new(0, 0.5)
        TextLabel.Position = UDim2.new(0.2400001734495163, 0, 0.5, 0)
        TextLabel.BackgroundTransparency = 1
        TextLabel.TextXAlignment = Enum.TextXAlignment.Left
        TextLabel.BorderSizePixel = 0
        TextLabel.BorderColor3 = Color3.fromRGB(50, 50, 50)
        TextLabel.TextSize = 13
        TextLabel.BackgroundColor3 = Color3.fromRGB(255,255,255)
        TextLabel.Parent = Tab
        
        local UIGradient = Instance.new('UIGradient')
        UIGradient.Color = ColorSequence.new{
            ColorSequenceKeypoint.new(0, Color3.fromRGB(255,255,255)),
            ColorSequenceKeypoint.new(0.7, Color3.fromRGB(155, 155, 155)),
            ColorSequenceKeypoint.new(1, Color3.fromRGB(58, 58, 58))
        }
        UIGradient.Parent = TextLabel
        
        local Icon = Instance.new('ImageLabel')
        Icon.ScaleType = Enum.ScaleType.Fit
        Icon.ImageTransparency = 0.800000011920929
        Icon.BorderColor3 = Color3.fromRGB(50, 50, 50)
        Icon.AnchorPoint = Vector2.new(0, 0.5)
        Icon.BackgroundTransparency = 1
        Icon.Position = UDim2.new(0.10000000149011612, 0, 0.5, 0)
        Icon.Name = 'Icon'
        Icon.Image = icon
        Icon.Size = UDim2.new(0, 12, 0, 12)
        Icon.BorderSizePixel = 0
        Icon.BackgroundColor3 = Color3.fromRGB(255,255,255)
        Icon.Parent = Tab

        local LeftSection = Instance.new('ScrollingFrame')
        LeftSection.Name = 'LeftSection'
        LeftSection.AutomaticCanvasSize = Enum.AutomaticSize.XY
        LeftSection.ScrollBarThickness = 0
        LeftSection.Size = UDim2.new(0, 243, 0, 445)
        LeftSection.Selectable = false
        LeftSection.AnchorPoint = Vector2.new(0, 0.5)
        LeftSection.ScrollBarImageTransparency = 1
        LeftSection.BackgroundTransparency = 1
        LeftSection.Position = UDim2.new(0.2594326436519623, 0, 0.5, 0)
        LeftSection.BorderColor3 = Color3.fromRGB(50, 50, 50)
        LeftSection.BackgroundColor3 = Color3.fromRGB(255,255,255)
        LeftSection.BorderSizePixel = 0
        LeftSection.CanvasSize = UDim2.new(0, 0, 0.5, 0)
        LeftSection.Visible = false
        LeftSection.Parent = Sections
        
        local UIListLayout = Instance.new('UIListLayout')
        UIListLayout.Padding = UDim.new(0, 11)
        UIListLayout.HorizontalAlignment = Enum.HorizontalAlignment.Center
        UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
        UIListLayout.Parent = LeftSection
        
        local UIPadding = Instance.new('UIPadding')
        UIPadding.PaddingTop = UDim.new(0, 1)
        UIPadding.Parent = LeftSection

        local RightSection = Instance.new('ScrollingFrame')
        RightSection.Name = 'RightSection'
        RightSection.AutomaticCanvasSize = Enum.AutomaticSize.XY
        RightSection.ScrollBarThickness = 0
        RightSection.Size = UDim2.new(0, 243, 0, 445)
        RightSection.Selectable = false
        RightSection.AnchorPoint = Vector2.new(0, 0.5)
        RightSection.ScrollBarImageTransparency = 1
        RightSection.BackgroundTransparency = 1
        RightSection.Position = UDim2.new(0.6290000081062317, 0, 0.5, 0)
        RightSection.BorderColor3 = Color3.fromRGB(50, 50, 50)
        RightSection.BackgroundColor3 = Color3.fromRGB(255,255,255)
        RightSection.BorderSizePixel = 0
        RightSection.CanvasSize = UDim2.new(0, 0, 0.5, 0)
        RightSection.Visible = false
        RightSection.Parent = Sections
        
        local UIListLayout = Instance.new('UIListLayout')
        UIListLayout.Padding = UDim.new(0, 11)
        UIListLayout.HorizontalAlignment = Enum.HorizontalAlignment.Center
        UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
        UIListLayout.Parent = RightSection
        
        local UIPadding = Instance.new('UIPadding')
        UIPadding.PaddingTop = UDim.new(0, 1)
        UIPadding.Parent = RightSection

        self._tab = self._tab + 1

        if first_tab then
            self:update_tabs(Tab, LeftSection, RightSection)
            self:update_sections(LeftSection, RightSection)
        end

        Tab.MouseButton1Click:Connect(function()
            self:update_tabs(Tab, LeftSection, RightSection)
            self:update_sections(LeftSection, RightSection)
        end)

        function TabManager:create_module(settings)

            local LayoutOrderModule = 0

            local ModuleManager = {
                _state = false,
                _size = 0,
                _multiplier = 0
            }

            if settings.section == 'right' then
                settings.section = RightSection
            else
                settings.section = LeftSection
            end

            local Module = Instance.new('Frame')
            Module.ClipsDescendants = true
            Module.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Module.BackgroundTransparency = 0.5
            Module.Position = UDim2.new(0.004115226212888956, 0, 0, 0)
            Module.Name = 'Module'
            Module.Size = UDim2.new(0, 241, 0, 93)
            Module.BorderSizePixel = 0
            Module.BackgroundColor3 = Color3.fromRGB(0,0,0)
            Module.Parent = settings.section

            local UIListLayout = Instance.new('UIListLayout')
            UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
            UIListLayout.Parent = Module
            
            local UICorner = Instance.new('UICorner')
            UICorner.CornerRadius = UDim.new(0, 5)
            UICorner.Parent = Module
            
            local UIStroke = Instance.new('UIStroke')
            UIStroke.Color = Color3.fromRGB(255,255,255)
            UIStroke.Transparency = 0.5
            UIStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
            UIStroke.Parent = Module
            
            local Header = Instance.new('TextButton')
            Header.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
            Header.TextColor3 = Color3.fromRGB(50, 50, 50)
            Header.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Header.Text = ''
            Header.AutoButtonColor = false
            Header.BackgroundTransparency = 1
            Header.Name = 'Header'
            Header.Size = UDim2.new(0, 241, 0, 93)
            Header.BorderSizePixel = 0
            Header.TextSize = 14
            Header.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Header.Parent = Module
            
            local Icon = Instance.new('ImageLabel')
            Icon.ImageColor3 = Color3.fromRGB(255,255,255)
            Icon.ScaleType = Enum.ScaleType.Fit
            Icon.ImageTransparency = 0.699999988079071
            Icon.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Icon.AnchorPoint = Vector2.new(0, 0.5)
            Icon.Image = 'rbxassetid://79095934438045'
            Icon.BackgroundTransparency = 1
            Icon.Position = UDim2.new(0.07100000232458115, 0, 0.8199999928474426, 0)
            Icon.Name = 'Icon'
            Icon.Size = UDim2.new(0, 15, 0, 15)
            Icon.BorderSizePixel = 0
            Icon.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Icon.Parent = Header
            
            local ModuleName = Instance.new('TextLabel')
            ModuleName.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
            ModuleName.TextColor3 = Color3.fromRGB(255,255,255)
            ModuleName.TextTransparency = 0.20000000298023224
            if not settings.rich then
                ModuleName.Text = settings.title or "Skibidi"
            else
                ModuleName.RichText = true
                ModuleName.Text = settings.richtext or "<font color='rgb(255,0,0)'>Skuwu</font> user"
            end
            ModuleName.Name = 'ModuleName'
            ModuleName.Size = UDim2.new(0, 205, 0, 13)
            ModuleName.AnchorPoint = Vector2.new(0, 0.5)
            ModuleName.Position = UDim2.new(0.0729999989271164, 0, 0.23999999463558197, 0)
            ModuleName.BackgroundTransparency = 1
            ModuleName.TextXAlignment = Enum.TextXAlignment.Left
            ModuleName.BorderSizePixel = 0
            ModuleName.BorderColor3 = Color3.fromRGB(50, 50, 50)
            ModuleName.TextSize = 13
            ModuleName.BackgroundColor3 = Color3.fromRGB(255,255,255)
            ModuleName.Parent = Header
            
            local Description = Instance.new('TextLabel')
            Description.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
            Description.TextColor3 = Color3.fromRGB(255,255,255)
            Description.TextTransparency = 0.699999988079071
            Description.Text = settings.description
            Description.Name = 'Description'
            Description.Size = UDim2.new(0, 205, 0, 13)
            Description.AnchorPoint = Vector2.new(0, 0.5)
            Description.Position = UDim2.new(0.0729999989271164, 0, 0.41999998688697815, 0)
            Description.BackgroundTransparency = 1
            Description.TextXAlignment = Enum.TextXAlignment.Left
            Description.BorderSizePixel = 0
            Description.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Description.TextSize = 10
            Description.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Description.Parent = Header
            
            local Toggle = Instance.new('Frame')
            Toggle.Name = 'Toggle'
            Toggle.BackgroundTransparency = 0.699999988079071
            Toggle.Position = UDim2.new(0.8199999928474426, 0, 0.7570000290870667, 0)
            Toggle.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Toggle.Size = UDim2.new(0, 25, 0, 12)
            Toggle.BorderSizePixel = 0
            Toggle.BackgroundColor3 = Color3.fromRGB(50, 50, 50)
            Toggle.Parent = Header
            
            local UICorner = Instance.new('UICorner')
            UICorner.CornerRadius = UDim.new(1, 0)
            UICorner.Parent = Toggle
            
            local Circle = Instance.new('Frame')
            Circle.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Circle.AnchorPoint = Vector2.new(0, 0.5)
            Circle.BackgroundTransparency = 0.20000000298023224
            Circle.Position = UDim2.new(0, 0, 0.5, 0)
            Circle.Name = 'Circle'
            Circle.Size = UDim2.new(0, 12, 0, 12)
            Circle.BorderSizePixel = 0
            Circle.BackgroundColor3 = Color3.fromRGB(150, 150, 150)
            Circle.Parent = Toggle
            
            local UICorner = Instance.new('UICorner')
            UICorner.CornerRadius = UDim.new(1, 0)
            UICorner.Parent = Circle
            
            local Keybind = Instance.new('Frame')
            Keybind.Name = 'Keybind'
            Keybind.BackgroundTransparency = 0.699999988079071
            Keybind.Position = UDim2.new(0.15000000596046448, 0, 0.7350000143051147, 0)
            Keybind.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Keybind.Size = UDim2.new(0, 33, 0, 15)
            Keybind.BorderSizePixel = 0
            Keybind.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Keybind.Parent = Header
            
            local UICorner = Instance.new('UICorner')
            UICorner.CornerRadius = UDim.new(0, 3)
            UICorner.Parent = Keybind
            
            local TextLabel = Instance.new('TextLabel')
            TextLabel.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
            TextLabel.TextColor3 = Color3.fromRGB(255,255,255)
            TextLabel.BorderColor3 = Color3.fromRGB(50, 50, 50)
            TextLabel.Text = 'None'
            TextLabel.AnchorPoint = Vector2.new(0.5, 0.5)
            TextLabel.Size = UDim2.new(0, 25, 0, 13)
            TextLabel.BackgroundTransparency = 1
            TextLabel.TextXAlignment = Enum.TextXAlignment.Left
            TextLabel.Position = UDim2.new(0.5, 0, 0.5, 0)
            TextLabel.BorderSizePixel = 0
            TextLabel.TextSize = 10
            TextLabel.BackgroundColor3 = Color3.fromRGB(255,255,255)
            TextLabel.Parent = Keybind
            
            local Divider = Instance.new('Frame')
            Divider.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Divider.AnchorPoint = Vector2.new(0.5, 0)
            Divider.BackgroundTransparency = 0.5
            Divider.Position = UDim2.new(0.5, 0, 0.6200000047683716, 0)
            Divider.Name = 'Divider'
            Divider.Size = UDim2.new(0, 241, 0, 1)
            Divider.BorderSizePixel = 0
            Divider.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Divider.Parent = Header
            
            local Divider = Instance.new('Frame')
            Divider.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Divider.AnchorPoint = Vector2.new(0.5, 0)
            Divider.BackgroundTransparency = 0.5
            Divider.Position = UDim2.new(0.5, 0, 1, 0)
            Divider.Name = 'Divider'
            Divider.Size = UDim2.new(0, 241, 0, 1)
            Divider.BorderSizePixel = 0
            Divider.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Divider.Parent = Header
            
            local Options = Instance.new('Frame')
            Options.Name = 'Options'
            Options.BackgroundTransparency = 1
            Options.Position = UDim2.new(0, 0, 1, 0)
            Options.BorderColor3 = Color3.fromRGB(50, 50, 50)
            Options.Size = UDim2.new(0, 241, 0, 8)
            Options.BorderSizePixel = 0
            Options.BackgroundColor3 = Color3.fromRGB(255,255,255)
            Options.Parent = Module

            local UIPadding = Instance.new('UIPadding')
            UIPadding.PaddingTop = UDim.new(0, 8)
            UIPadding.Parent = Options

            local UIListLayout = Instance.new('UIListLayout')
            UIListLayout.Padding = UDim.new(0, 5)
            UIListLayout.HorizontalAlignment = Enum.HorizontalAlignment.Center
            UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
            UIListLayout.Parent = Options

            function ModuleManager:change_state(state)
                self._state = state

                if self._state then
                    TweenService:Create(Module, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        Size = UDim2.fromOffset(241, 93 + self._size + self._multiplier)
                    }):Play()

                    TweenService:Create(Toggle, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(255,255,255)
                    }):Play()

                    TweenService:Create(Circle, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(255,255,255),
                        Position = UDim2.fromScale(0.53, 0.5)
                    }):Play()
                else
                    TweenService:Create(Module, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        Size = UDim2.fromOffset(241, 93)
                    }):Play()

                    TweenService:Create(Toggle, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(50, 50, 50)
                    }):Play()

                    TweenService:Create(Circle, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(150, 150, 150),
                        Position = UDim2.fromScale(0, 0.5)
                    }):Play()
                end

                Library._config._flags[settings.flag] = self._state
                Config:save(game.GameId, Library._config)

                settings.callback(self._state)
            end
            
            function ModuleManager:connect_keybind()
                if not Library._config._keybinds[settings.flag] then
                    return
                end

                Connections[settings.flag..'_keybind'] = UserInputService.InputBegan:Connect(function(input, process)
                    if process then
                        return
                    end
                    
                    if tostring(input.KeyCode) ~= Library._config._keybinds[settings.flag] then
                        return
                    end
                    
                    self:change_state(not self._state)
                end)
            end

            function ModuleManager:scale_keybind(empty)
                if Library._config._keybinds[settings.flag] and not empty then
                    local keybind_string = string.gsub(tostring(Library._config._keybinds[settings.flag]), 'Enum.KeyCode.', '')

                    local font_params = Instance.new('GetTextBoundsParams')
                    font_params.Text = keybind_string
                    font_params.Font = Font.new('rbxasset://fonts/families/Montserrat.json', Enum.FontWeight.Bold)
                    font_params.Size = 10
                    font_params.Width = 10000
            
                    local font_size = TextService:GetTextBoundsAsync(font_params)
                    
                    Keybind.Size = UDim2.fromOffset(font_size.X + 6, 15)
                    TextLabel.Size = UDim2.fromOffset(font_size.X, 13)
                else
                    Keybind.Size = UDim2.fromOffset(31, 15)
                    TextLabel.Size = UDim2.fromOffset(25, 13)
                end
            end

            if Library:flag_type(settings.flag, 'boolean') then
                ModuleManager._state = true
                settings.callback(ModuleManager._state)

                Toggle.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Circle.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Circle.Position = UDim2.fromScale(0.53, 0.5)
            end

            if Library._config._keybinds[settings.flag] then
                local keybind_string = string.gsub(tostring(Library._config._keybinds[settings.flag]), 'Enum.KeyCode.', '')
                TextLabel.Text = keybind_string

                ModuleManager:connect_keybind()
                ModuleManager:scale_keybind()
            end

            Connections[settings.flag..'_input_began'] = Header.InputBegan:Connect(function(input)
                if Library._choosing_keybind then
                    return
                end

                if input.UserInputType ~= Enum.UserInputType.MouseButton3 then
                    return
                end
                
                Library._choosing_keybind = true
                
                Connections['keybind_choose_start'] = UserInputService.InputBegan:Connect(function(input, process)
                    if process then
                        return
                    end
                    
                    if input == Enum.UserInputState or input == Enum.UserInputType then
                        return
                    end

                    if input.KeyCode == Enum.KeyCode.Unknown then
                        return
                    end

                    if input.KeyCode == Enum.KeyCode.Backspace then
                        ModuleManager:scale_keybind(true)

                        Library._config._keybinds[settings.flag] = nil
                        Config:save(game.GameId, Library._config)

                        TextLabel.Text = 'None'
                        
                        if Connections[settings.flag..'_keybind'] then
                            Connections[settings.flag..'_keybind']:Disconnect()
                            Connections[settings.flag..'_keybind'] = nil
                        end

                        Connections['keybind_choose_start']:Disconnect()
                        Connections['keybind_choose_start'] = nil

                        Library._choosing_keybind = false

                        return
                    end
                    
                    Connections['keybind_choose_start']:Disconnect()
                    Connections['keybind_choose_start'] = nil
                    
                    Library._config._keybinds[settings.flag] = tostring(input.KeyCode)
                    Config:save(game.GameId, Library._config)

                    if Connections[settings.flag..'_keybind'] then
                        Connections[settings.flag..'_keybind']:Disconnect()
                        Connections[settings.flag..'_keybind'] = nil
                    end

                    ModuleManager:connect_keybind()
                    ModuleManager:scale_keybind()
                    
                    Library._choosing_keybind = false

                    local keybind_string = string.gsub(tostring(Library._config._keybinds[settings.flag]), 'Enum.KeyCode.', '')
                    TextLabel.Text = keybind_string
                end)
            end)

            Header.MouseButton1Click:Connect(function()
                ModuleManager:change_state(not ModuleManager._state)
            end)

            function ModuleManager:create_paragraph(settings)
                LayoutOrderModule = LayoutOrderModule + 1

                local ParagraphManager = {}
                
                if self._size == 0 then
                    self._size = 11
                end
            
                self._size = self._size + (settings.customScale or 70)
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end
            
                Options.Size = UDim2.fromOffset(241, self._size)
            
                local Paragraph = Instance.new('Frame')
                Paragraph.BackgroundColor3 = Color3.fromRGB(0,0,0)
                Paragraph.BackgroundTransparency = 0.1
                Paragraph.Size = UDim2.new(0, 207, 0, 30)
                Paragraph.BorderSizePixel = 0
                Paragraph.Name = "Paragraph"
                Paragraph.AutomaticSize = Enum.AutomaticSize.Y
                Paragraph.Parent = Options
                Paragraph.LayoutOrder = LayoutOrderModule
            
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(0, 4)
                UICorner.Parent = Paragraph
            
                local Title = Instance.new('TextLabel')
                Title.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                Title.TextColor3 = Color3.fromRGB(255,255,255)
                Title.Text = settings.title or "Title"
                Title.Size = UDim2.new(1, -10, 0, 20)
                Title.Position = UDim2.new(0, 5, 0, 5)
                Title.BackgroundTransparency = 1
                Title.TextXAlignment = Enum.TextXAlignment.Left
                Title.TextYAlignment = Enum.TextYAlignment.Center
                Title.TextSize = 12
                Title.AutomaticSize = Enum.AutomaticSize.XY
                Title.Parent = Paragraph
            
                local Body = Instance.new('TextLabel')
                Body.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Body.TextColor3 = Color3.fromRGB(180, 180, 180)
                
                if not settings.rich then
                    Body.Text = settings.text or "Skibidi"
                else
                    Body.RichText = true
                    Body.Text = settings.richtext or "<font color='rgb(255,0,0)'>Skuwu</font> user"
                end
                
                Body.Size = UDim2.new(1, -10, 0, 20)
                Body.Position = UDim2.new(0, 5, 0, 30)
                Body.BackgroundTransparency = 1
                Body.TextXAlignment = Enum.TextXAlignment.Left
                Body.TextYAlignment = Enum.TextYAlignment.Top
                Body.TextSize = 11
                Body.TextWrapped = true
                Body.AutomaticSize = Enum.AutomaticSize.XY
                Body.Parent = Paragraph
            
                Paragraph.MouseEnter:Connect(function()
                    TweenService:Create(Paragraph, TweenInfo.new(0.3, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(100, 100, 100)
                    }):Play()
                end)
            
                Paragraph.MouseLeave:Connect(function()
                    TweenService:Create(Paragraph, TweenInfo.new(0.3, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(0,0,0)
                    }):Play()
                end)

                return ParagraphManager
            end

            function ModuleManager:create_text(settings)
                LayoutOrderModule = LayoutOrderModule + 1
            
                local TextManager = {}
            
                if self._size == 0 then
                    self._size = 11
                end
            
                self._size = self._size + (settings.customScale or 50)
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end
            
                Options.Size = UDim2.fromOffset(241, self._size)
            
                local TextFrame = Instance.new('Frame')
                TextFrame.BackgroundColor3 = Color3.fromRGB(0,0,0)
                TextFrame.BackgroundTransparency = 0.1
                TextFrame.Size = UDim2.new(0, 207, 0, settings.CustomYSize)
                TextFrame.BorderSizePixel = 0
                TextFrame.Name = "Text"
                TextFrame.AutomaticSize = Enum.AutomaticSize.Y
                TextFrame.Parent = Options
                TextFrame.LayoutOrder = LayoutOrderModule
            
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(0, 4)
                UICorner.Parent = TextFrame
            
                local Body = Instance.new('TextLabel')
                Body.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Body.TextColor3 = Color3.fromRGB(180, 180, 180)
            
                if not settings.rich then
                    Body.Text = settings.text or "Skibidi"
                else
                    Body.RichText = true
                    Body.Text = settings.richtext or "<font color='rgb(255,0,0)'>Skuwu</font> user"
                end
            
                Body.Size = UDim2.new(1, -10, 1, 0)
                Body.Position = UDim2.new(0, 5, 0, 5)
                Body.BackgroundTransparency = 1
                Body.TextXAlignment = Enum.TextXAlignment.Left
                Body.TextYAlignment = Enum.TextYAlignment.Top
                Body.TextSize = 10
                Body.TextWrapped = true
                Body.AutomaticSize = Enum.AutomaticSize.XY
                Body.Parent = TextFrame
            
                TextFrame.MouseEnter:Connect(function()
                    TweenService:Create(TextFrame, TweenInfo.new(0.3, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(100, 100, 100)
                    }):Play()
                end)
            
                TextFrame.MouseLeave:Connect(function()
                    TweenService:Create(TextFrame, TweenInfo.new(0.3, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        BackgroundColor3 = Color3.fromRGB(0,0,0)
                    }):Play()
                end)

                function TextManager:Set(new_settings)
                    if not new_settings.rich then
                        Body.Text = new_settings.text or "Skibidi"
                    else
                        Body.RichText = true
                        Body.Text = new_settings.richtext or "<font color='rgb(255,0,0)'>Skuwu</font> user"
                    end
                end
            
                return TextManager
            end

            function ModuleManager:create_textbox(settings)
                LayoutOrderModule = LayoutOrderModule + 1
            
                local TextboxManager = {
                    _text = ""
                }
            
                if self._size == 0 then
                    self._size = 11
                end
            
                self._size = self._size + 32
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end
            
                Options.Size = UDim2.fromOffset(241, self._size)
            
                local Label = Instance.new('TextLabel')
                Label.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                Label.TextColor3 = Color3.fromRGB(255,255,255)
                Label.TextTransparency = 0.2
                Label.Text = settings.title or "Enter text"
                Label.Size = UDim2.new(0, 207, 0, 13)
                Label.AnchorPoint = Vector2.new(0, 0)
                Label.Position = UDim2.new(0, 0, 0, 0)
                Label.BackgroundTransparency = 1
                Label.TextXAlignment = Enum.TextXAlignment.Left
                Label.BorderSizePixel = 0
                Label.Parent = Options
                Label.TextSize = 10
                Label.LayoutOrder = LayoutOrderModule
            
                local Textbox = Instance.new('TextBox')
                Textbox.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Textbox.TextColor3 = Color3.fromRGB(255,255,255)
                Textbox.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Textbox.PlaceholderText = settings.placeholder or "Enter text..."
                Textbox.Text = Library._config._flags[settings.flag] or ""
                Textbox.Name = 'Textbox'
                Textbox.Size = UDim2.new(0, 207, 0, 15)
                Textbox.BorderSizePixel = 0
                Textbox.TextSize = 10
                Textbox.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Textbox.BackgroundTransparency = 0.9
                Textbox.ClearTextOnFocus = false
                Textbox.Parent = Options
                Textbox.LayoutOrder = LayoutOrderModule
            
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(0, 4)
                UICorner.Parent = Textbox
            
                function TextboxManager:update_text(text)
                    self._text = text
                    Library._config._flags[settings.flag] = self._text
                    Config:save(game.GameId, Library._config)
                    settings.callback(self._text)
                end
            
                if Library:flag_type(settings.flag, 'string') then
                    TextboxManager:update_text(Library._config._flags[settings.flag])
                end
            
                Textbox.FocusLost:Connect(function()
                    TextboxManager:update_text(Textbox.Text)
                end)
            
                return TextboxManager
            end   

            function ModuleManager:create_checkbox(settings)
                LayoutOrderModule = LayoutOrderModule + 1
                local CheckboxManager = { _state = false }
            
                if self._size == 0 then
                    self._size = 11
                end
                self._size = self._size + 20
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end
                Options.Size = UDim2.fromOffset(241, self._size)
            
                local Checkbox = Instance.new("TextButton")
                Checkbox.FontFace = Font.new("rbxasset://fonts/families/SourceSansPro.json", Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Checkbox.TextColor3 = Color3.fromRGB(50, 50, 50)
                Checkbox.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Checkbox.Text = ""
                Checkbox.AutoButtonColor = false
                Checkbox.BackgroundTransparency = 1
                Checkbox.Name = "Checkbox"
                Checkbox.Size = UDim2.new(0, 207, 0, 15)
                Checkbox.BorderSizePixel = 0
                Checkbox.TextSize = 14
                Checkbox.BackgroundColor3 = Color3.fromRGB(50, 50, 50)
                Checkbox.Parent = Options
                Checkbox.LayoutOrder = LayoutOrderModule
            
                local TitleLabel = Instance.new("TextLabel")
                TitleLabel.Name = "TitleLabel"
                if SelectedLanguage == "th" then
                    TitleLabel.FontFace = Font.new("rbxasset://fonts/families/NotoSansThai.json", Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TitleLabel.TextSize = 13
                else
                    TitleLabel.FontFace = Font.new("rbxasset://fonts/families/GothamSSm.json", Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TitleLabel.TextSize = 11
                end
                TitleLabel.TextColor3 = Color3.fromRGB(255,255,255)
                TitleLabel.TextTransparency = 0.2
                TitleLabel.Text = settings.title or "Skibidi"
                TitleLabel.Size = UDim2.new(0, 142, 0, 13)
                TitleLabel.AnchorPoint = Vector2.new(0, 0.5)
                TitleLabel.Position = UDim2.new(0, 0, 0.5, 0)
                TitleLabel.BackgroundTransparency = 1
                TitleLabel.TextXAlignment = Enum.TextXAlignment.Left
                TitleLabel.Parent = Checkbox

                local KeybindBox = Instance.new("Frame")
                KeybindBox.Name = "KeybindBox"
                KeybindBox.Size = UDim2.fromOffset(14, 14)
                KeybindBox.Position = UDim2.new(1, -35, 0.5, 0)
                KeybindBox.AnchorPoint = Vector2.new(0, 0.5)
                KeybindBox.BackgroundColor3 = Color3.fromRGB(255,255,255)
                KeybindBox.BorderSizePixel = 0
                KeybindBox.Parent = Checkbox
            
                local KeybindCorner = Instance.new("UICorner")
                KeybindCorner.CornerRadius = UDim.new(0, 4)
                KeybindCorner.Parent = KeybindBox
            
                local KeybindLabel = Instance.new("TextLabel")
                KeybindLabel.Name = "KeybindLabel"
                KeybindLabel.Size = UDim2.new(1, 0, 1, 0)
                KeybindLabel.BackgroundTransparency = 1
                KeybindLabel.TextColor3 = Color3.fromRGB(50, 50, 50)
                KeybindLabel.TextScaled = false
                KeybindLabel.TextSize = 10
                KeybindLabel.Font = Enum.Font.SourceSans
                KeybindLabel.Text = Library._config._keybinds[settings.flag] 
                    and string.gsub(tostring(Library._config._keybinds[settings.flag]), "Enum.KeyCode.", "") 
                    or "..."
                KeybindLabel.Parent = KeybindBox
            
                local Box = Instance.new("Frame")
                Box.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Box.AnchorPoint = Vector2.new(1, 0.5)
                Box.BackgroundTransparency = 0.9
                Box.Position = UDim2.new(1, 0, 0.5, 0)
                Box.Name = "Box"
                Box.Size = UDim2.new(0, 15, 0, 15)
                Box.BorderSizePixel = 0
                Box.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Box.Parent = Checkbox
            
                local BoxCorner = Instance.new("UICorner")
                BoxCorner.CornerRadius = UDim.new(0, 4)
                BoxCorner.Parent = Box
            
                local Fill = Instance.new("Frame")
                Fill.AnchorPoint = Vector2.new(0.5, 0.5)
                Fill.BackgroundTransparency = 0.2
                Fill.Position = UDim2.new(0.5, 0, 0.5, 0)
                Fill.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Fill.Name = "Fill"
                Fill.BorderSizePixel = 0
                Fill.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Fill.Parent = Box
            
                local FillCorner = Instance.new("UICorner")
                FillCorner.CornerRadius = UDim.new(0, 3)
                FillCorner.Parent = Fill
            
                function CheckboxManager:change_state(state)
                    self._state = state
                    if self._state then
                        TweenService:Create(Box, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            BackgroundTransparency = 0.7
                        }):Play()
                        TweenService:Create(Fill, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(9, 9)
                        }):Play()
                    else
                        TweenService:Create(Box, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            BackgroundTransparency = 0.9
                        }):Play()
                        TweenService:Create(Fill, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(0, 0)
                        }):Play()
                    end
                    Library._config._flags[settings.flag] = self._state
                    Config:save(game.GameId, Library._config)
                    settings.callback(self._state)
                end
            
                if Library:flag_type(settings.flag, "boolean") then
                    CheckboxManager:change_state(Library._config._flags[settings.flag])
                end
            
                Checkbox.MouseButton1Click:Connect(function()
                    CheckboxManager:change_state(not CheckboxManager._state)
                end)
            
                Checkbox.InputBegan:Connect(function(input, gameProcessed)
                    if gameProcessed then return end
                    if input.UserInputType ~= Enum.UserInputType.MouseButton3 then return end
                    if Library._choosing_keybind then return end
            
                    Library._choosing_keybind = true
                    local chooseConnection
                    chooseConnection = UserInputService.InputBegan:Connect(function(keyInput, processed)
                        if processed then return end
                        if keyInput.UserInputType ~= Enum.UserInputType.Keyboard then return end
                        if keyInput.KeyCode == Enum.KeyCode.Unknown then return end
            
                        if keyInput.KeyCode == Enum.KeyCode.Backspace then
                            ModuleManager:scale_keybind(true)
                            Library._config._keybinds[settings.flag] = nil
                            Config:save(game.GameId, Library._config)
                            KeybindLabel.Text = "..."
                            if Connections[settings.flag .. "_keybind"] then
                                Connections[settings.flag .. "_keybind"]:Disconnect()
                                Connections[settings.flag .. "_keybind"] = nil
                            end
                            chooseConnection:Disconnect()
                            Library._choosing_keybind = false
                            return
                        end
            
                        chooseConnection:Disconnect()
                        Library._config._keybinds[settings.flag] = tostring(keyInput.KeyCode)
                        Config:save(game.GameId, Library._config)
                        if Connections[settings.flag .. "_keybind"] then
                            Connections[settings.flag .. "_keybind"]:Disconnect()
                            Connections[settings.flag .. "_keybind"] = nil
                        end
                        ModuleManager:connect_keybind()
                        ModuleManager:scale_keybind()
                        Library._choosing_keybind = false
            
                        local keybind_string = string.gsub(tostring(Library._config._keybinds[settings.flag]), "Enum.KeyCode.", "")
                        KeybindLabel.Text = keybind_string
                    end)
                end)
            
                local keyPressConnection = UserInputService.InputBegan:Connect(function(input, gameProcessed)
                    if gameProcessed then return end
                    if input.UserInputType == Enum.UserInputType.Keyboard then
                        local storedKey = Library._config._keybinds[settings.flag]
                        if storedKey and tostring(input.KeyCode) == storedKey then
                            CheckboxManager:change_state(not CheckboxManager._state)
                        end
                    end
                end)
                Connections[settings.flag .. "_keypress"] = keyPressConnection
            
                return CheckboxManager
            end

            function ModuleManager:create_divider(settings)
                LayoutOrderModule = LayoutOrderModule + 1
            
                if self._size == 0 then
                    self._size = 11
                end
            
                self._size = self._size + 27
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end

                local dividerHeight = 1
                local dividerWidth = 207
            
                local OuterFrame = Instance.new('Frame')
                OuterFrame.Size = UDim2.new(0, dividerWidth, 0, 20)
                OuterFrame.BackgroundTransparency = 1
                OuterFrame.Name = 'OuterFrame'
                OuterFrame.Parent = Options
                OuterFrame.LayoutOrder = LayoutOrderModule

                if settings and settings.showtopic then
                    local TextLabel = Instance.new('TextLabel')
                    TextLabel.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TextLabel.TextColor3 = Color3.fromRGB(255,255,255)
                    TextLabel.TextTransparency = 0
                    TextLabel.Text = settings.title
                    TextLabel.Size = UDim2.new(0, 153, 0, 13)
                    TextLabel.Position = UDim2.new(0.5, 0, 0.501, 0)
                    TextLabel.BackgroundTransparency = 1
                    TextLabel.TextXAlignment = Enum.TextXAlignment.Center
                    TextLabel.BorderSizePixel = 0
                    TextLabel.AnchorPoint = Vector2.new(0.5,0.5)
                    TextLabel.BorderColor3 = Color3.fromRGB(50, 50, 50)
                    TextLabel.TextSize = 11
                    TextLabel.BackgroundColor3 = Color3.fromRGB(255,255,255)
                    TextLabel.ZIndex = 3
                    TextLabel.TextStrokeTransparency = 0
                    TextLabel.Parent = OuterFrame
                end
                
                if not settings or settings and not settings.disableline then
                    local Divider = Instance.new('Frame')
                    Divider.Size = UDim2.new(1, 0, 0, dividerHeight)
                    Divider.BackgroundColor3 = Color3.fromRGB(255,255,255)
                    Divider.BorderSizePixel = 0
                    Divider.Name = 'Divider'
                    Divider.Parent = OuterFrame
                    Divider.ZIndex = 2
                    Divider.Position = UDim2.new(0, 0, 0.5, -dividerHeight / 2)
                
                    local Gradient = Instance.new('UIGradient')
                    Gradient.Parent = Divider
                    Gradient.Color = ColorSequence.new({
                        ColorSequenceKeypoint.new(0, Color3.fromRGB(255,255,255)),
                        ColorSequenceKeypoint.new(0.5, Color3.fromRGB(255,255,255)),
                        ColorSequenceKeypoint.new(1, Color3.fromRGB(255,255,255))
                    })
                    Gradient.Transparency = NumberSequence.new({
                        NumberSequenceKeypoint.new(0, 1),   
                        NumberSequenceKeypoint.new(0.5, 0),
                        NumberSequenceKeypoint.new(1, 1)
                    })
                    Gradient.Rotation = 0
                
                    local UICorner = Instance.new('UICorner')
                    UICorner.CornerRadius = UDim.new(0, 2)
                    UICorner.Parent = Divider
                end
            
                return true
            end
            
            function ModuleManager:create_slider(settings)

                LayoutOrderModule = LayoutOrderModule + 1

                local SliderManager = {}

                if self._size == 0 then
                    self._size = 11
                end

                self._size = self._size + 27

                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end

                Options.Size = UDim2.fromOffset(241, self._size)

                local Slider = Instance.new('TextButton')
                Slider.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Slider.TextSize = 14
                Slider.TextColor3 = Color3.fromRGB(50, 50, 50)
                Slider.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Slider.Text = ''
                Slider.AutoButtonColor = false
                Slider.BackgroundTransparency = 1
                Slider.Name = 'Slider'
                Slider.Size = UDim2.new(0, 207, 0, 22)
                Slider.BorderSizePixel = 0
                Slider.BackgroundColor3 = Color3.fromRGB(50, 50, 50)
                Slider.Parent = Options
                Slider.LayoutOrder = LayoutOrderModule
                
                local TextLabel = Instance.new('TextLabel')
                if GG.SelectedLanguage == "th" then
                    TextLabel.FontFace = Font.new("rbxasset://fonts/families/NotoSansThai.json", Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TextLabel.TextSize = 13
                else
                    TextLabel.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TextLabel.TextSize = 11
                end
                TextLabel.TextColor3 = Color3.fromRGB(255,255,255)
                TextLabel.TextTransparency = 0.20000000298023224
                TextLabel.Text = settings.title
                TextLabel.Size = UDim2.new(0, 153, 0, 13)
                TextLabel.Position = UDim2.new(0, 0, 0.05000000074505806, 0)
                TextLabel.BackgroundTransparency = 1
                TextLabel.TextXAlignment = Enum.TextXAlignment.Left
                TextLabel.BorderSizePixel = 0
                TextLabel.BorderColor3 = Color3.fromRGB(50, 50, 50)
                TextLabel.BackgroundColor3 = Color3.fromRGB(255,255,255)
                TextLabel.Parent = Slider
                
                local Drag = Instance.new('Frame')
                Drag.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Drag.AnchorPoint = Vector2.new(0.5, 1)
                Drag.BackgroundTransparency = 0.8999999761581421
                Drag.Position = UDim2.new(0.5, 0, 0.949999988079071, 0)
                Drag.Name = 'Drag'
                Drag.Size = UDim2.new(0, 207, 0, 4)
                Drag.BorderSizePixel = 0
                Drag.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Drag.Parent = Slider
                
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(1, 0)
                UICorner.Parent = Drag
                
                local Fill = Instance.new('Frame')
                Fill.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Fill.AnchorPoint = Vector2.new(0, 0.5)
                Fill.BackgroundTransparency = 0.5
                Fill.Position = UDim2.new(0, 0, 0.5, 0)
                Fill.Name = 'Fill'
                Fill.Size = UDim2.new(0, 103, 0, 4)
                Fill.BorderSizePixel = 0
                Fill.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Fill.Parent = Drag
                
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(0, 3)
                UICorner.Parent = Fill
                
                local UIGradient = Instance.new('UIGradient')
                UIGradient.Color = ColorSequence.new{
                    ColorSequenceKeypoint.new(0, Color3.fromRGB(255,255,255)),
                    ColorSequenceKeypoint.new(1, Color3.fromRGB(79, 79, 79))
                }
                UIGradient.Parent = Fill
                
                local Circle = Instance.new('Frame')
                Circle.AnchorPoint = Vector2.new(1, 0.5)
                Circle.Name = 'Circle'
                Circle.Position = UDim2.new(1, 0, 0.5, 0)
                Circle.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Circle.Size = UDim2.new(0, 6, 0, 6)
                Circle.BorderSizePixel = 0
                Circle.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Circle.Parent = Fill
                
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(1, 0)
                UICorner.Parent = Circle
                
                local Value = Instance.new('TextLabel')
                Value.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                Value.TextColor3 = Color3.fromRGB(255,255,255)
                Value.TextTransparency = 0.20000000298023224
                Value.Text = '50'
                Value.Name = 'Value'
                Value.Size = UDim2.new(0, 42, 0, 13)
                Value.AnchorPoint = Vector2.new(1, 0)
                Value.Position = UDim2.new(1, 0, 0, 0)
                Value.BackgroundTransparency = 1
                Value.TextXAlignment = Enum.TextXAlignment.Right
                Value.BorderSizePixel = 0
                Value.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Value.TextSize = 10
                Value.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Value.Parent = Slider

                function SliderManager:set_percentage(percentage)
                    local rounded_number = 0

                    if settings.round_number then
                        rounded_number = math.floor(percentage)
                    else
                        rounded_number = math.floor(percentage * 10) / 10
                    end

                    percentage = (percentage - settings.minimum_value) / (settings.maximum_value - settings.minimum_value)
                    
                    local slider_size = math.clamp(percentage, 0.02, 1) * Drag.Size.X.Offset
                    local number_threshold = math.clamp(rounded_number, settings.minimum_value, settings.maximum_value)
    
                    Library._config._flags[settings.flag] = number_threshold
                    Value.Text = number_threshold
    
                    TweenService:Create(Fill, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                        Size = UDim2.fromOffset(slider_size, Drag.Size.Y.Offset)
                    }):Play()
    
                    settings.callback(number_threshold)
                end

                function SliderManager:update()
                    local mouse_position = (mouse.X - Drag.AbsolutePosition.X) / Drag.Size.X.Offset
                    local percentage = settings.minimum_value + (settings.maximum_value - settings.minimum_value) * mouse_position

                    self:set_percentage(percentage)
                end

                function SliderManager:input()
                    SliderManager:update()
    
                    Connections['slider_drag_'..settings.flag] = mouse.Move:Connect(function()
                        SliderManager:update()
                    end)
                    
                    Connections['slider_input_'..settings.flag] = UserInputService.InputEnded:Connect(function(input, process)
                        if input.UserInputType ~= Enum.UserInputType.MouseButton1 and input.UserInputType ~= Enum.UserInputType.Touch then
                            return
                        end
    
                        Connections:disconnect('slider_drag_'..settings.flag)
                        Connections:disconnect('slider_input_'..settings.flag)

                        if not settings.ignoresaved then
                            Config:save(game.GameId, Library._config)
                        end
                    end)
                end


                if Library:flag_type(settings.flag, 'number') then
                    if not settings.ignoresaved then
                        SliderManager:set_percentage(Library._config._flags[settings.flag])
                    else
                        SliderManager:set_percentage(settings.value)
                    end
                else
                    SliderManager:set_percentage(settings.value)
                end
    
                Slider.MouseButton1Down:Connect(function()
                    SliderManager:input()
                end)

                return SliderManager
            end

            function ModuleManager:create_dropdown(settings)

                if not settings.Order then
                    LayoutOrderModule = LayoutOrderModule + 1
                end

                local DropdownManager = {
                    _state = false,
                    _size = 0
                }

                if not settings.Order then
                    if self._size == 0 then
                        self._size = 11
                    end

                    self._size = self._size + 44
                end

                if not settings.Order then
                    if ModuleManager._state then
                        Module.Size = UDim2.fromOffset(241, 93 + self._size)
                    end
                    Options.Size = UDim2.fromOffset(241, self._size)
                end

                local Dropdown = Instance.new('TextButton')
                Dropdown.FontFace = Font.new('rbxasset://fonts/families/SourceSansPro.json', Enum.FontWeight.Regular, Enum.FontStyle.Normal)
                Dropdown.TextColor3 = Color3.fromRGB(50, 50, 50)
                Dropdown.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Dropdown.Text = ''
                Dropdown.AutoButtonColor = false
                Dropdown.BackgroundTransparency = 1
                Dropdown.Name = 'Dropdown'
                Dropdown.Size = UDim2.new(0, 207, 0, 39)
                Dropdown.BorderSizePixel = 0
                Dropdown.TextSize = 14
                Dropdown.BackgroundColor3 = Color3.fromRGB(50, 50, 50)
                Dropdown.Parent = Options

                if not settings.Order then
                    Dropdown.LayoutOrder = LayoutOrderModule
                else
                    Dropdown.LayoutOrder = settings.OrderValue
                end

                if not Library._config._flags[settings.flag] then
                    Library._config._flags[settings.flag] = {}
                end
                
                local TextLabel = Instance.new('TextLabel')
                if GG.SelectedLanguage == "th" then
                    TextLabel.FontFace = Font.new("rbxasset://fonts/families/NotoSansThai.json", Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TextLabel.TextSize = 13
                else
                    TextLabel.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                    TextLabel.TextSize = 11
                end
                TextLabel.TextColor3 = Color3.fromRGB(255,255,255)
                TextLabel.TextTransparency = 0.20000000298023224
                TextLabel.Text = settings.title
                TextLabel.Size = UDim2.new(0, 207, 0, 13)
                TextLabel.BackgroundTransparency = 1
                TextLabel.TextXAlignment = Enum.TextXAlignment.Left
                TextLabel.BorderSizePixel = 0
                TextLabel.BorderColor3 = Color3.fromRGB(50, 50, 50)
                TextLabel.BackgroundColor3 = Color3.fromRGB(255,255,255)
                TextLabel.Parent = Dropdown
                
                local Box = Instance.new('Frame')
                Box.ClipsDescendants = true
                Box.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Box.AnchorPoint = Vector2.new(0.5, 0)
                Box.BackgroundTransparency = 0.8999999761581421
                Box.Position = UDim2.new(0.5, 0, 1.2000000476837158, 0)
                Box.Name = 'Box'
                Box.Size = UDim2.new(0, 207, 0, 22)
                Box.BorderSizePixel = 0
                Box.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Box.Parent = TextLabel
                
                local UICorner = Instance.new('UICorner')
                UICorner.CornerRadius = UDim.new(0, 4)
                UICorner.Parent = Box
                
                local Header = Instance.new('Frame')
                Header.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Header.AnchorPoint = Vector2.new(0.5, 0)
                Header.BackgroundTransparency = 1
                Header.Position = UDim2.new(0.5, 0, 0, 0)
                Header.Name = 'Header'
                Header.Size = UDim2.new(0, 207, 0, 22)
                Header.BorderSizePixel = 0
                Header.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Header.Parent = Box
                
                local CurrentOption = Instance.new('TextLabel')
                CurrentOption.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                CurrentOption.TextColor3 = Color3.fromRGB(255,255,255)
                CurrentOption.TextTransparency = 0.20000000298023224
                CurrentOption.Name = 'CurrentOption'
                CurrentOption.Size = UDim2.new(0, 161, 0, 13)
                CurrentOption.AnchorPoint = Vector2.new(0, 0.5)
                CurrentOption.Position = UDim2.new(0.04999988153576851, 0, 0.5, 0)
                CurrentOption.BackgroundTransparency = 1
                CurrentOption.TextXAlignment = Enum.TextXAlignment.Left
                CurrentOption.BorderSizePixel = 0
                CurrentOption.BorderColor3 = Color3.fromRGB(50, 50, 50)
                CurrentOption.TextSize = 10
                CurrentOption.BackgroundColor3 = Color3.fromRGB(255,255,255)
                CurrentOption.Parent = Header
                local UIGradient = Instance.new('UIGradient')
                UIGradient.Transparency = NumberSequence.new{
                    NumberSequenceKeypoint.new(0, 0),
                    NumberSequenceKeypoint.new(0.704, 0),
                    NumberSequenceKeypoint.new(0.872, 0.36250001192092896),
                    NumberSequenceKeypoint.new(1, 1)
                }
                UIGradient.Parent = CurrentOption
                
                local Arrow = Instance.new('ImageLabel')
                Arrow.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Arrow.AnchorPoint = Vector2.new(0, 0.5)
                Arrow.Image = 'rbxassetid://84232453189324'
                Arrow.BackgroundTransparency = 1
                Arrow.Position = UDim2.new(0.9100000262260437, 0, 0.5, 0)
                Arrow.Name = 'Arrow'
                Arrow.Size = UDim2.new(0, 8, 0, 8)
                Arrow.BorderSizePixel = 0
                Arrow.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Arrow.Parent = Header
                
                local Options = Instance.new('ScrollingFrame')
                Options.ScrollBarImageColor3 = Color3.fromRGB(50, 50, 50)
                Options.Active = true
                Options.ScrollBarImageTransparency = 1
                Options.AutomaticCanvasSize = Enum.AutomaticSize.XY
                Options.ScrollBarThickness = 0
                Options.Name = 'Options'
                Options.Size = UDim2.new(0, 207, 0, 0)
                Options.BackgroundTransparency = 1
                Options.Position = UDim2.new(0, 0, 1, 0)
                Options.BackgroundColor3 = Color3.fromRGB(255,255,255)
                Options.BorderColor3 = Color3.fromRGB(50, 50, 50)
                Options.BorderSizePixel = 0
                Options.CanvasSize = UDim2.new(0, 0, 0.5, 0)
                Options.Parent = Box
                
                local UIListLayout = Instance.new('UIListLayout')
                UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
                UIListLayout.Parent = Options
                
                local UIPadding = Instance.new('UIPadding')
                UIPadding.PaddingTop = UDim.new(0, -1)
                UIPadding.PaddingLeft = UDim.new(0, 10)
                UIPadding.Parent = Options
                
                local UIListLayout = Instance.new('UIListLayout')
                UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
                UIListLayout.Parent = Box

                function DropdownManager:update(option)
                    if settings.multi_dropdown then
                        if not Library._config._flags[settings.flag] then
                            Library._config._flags[settings.flag] = {}
                        end

                        local CurrentTargetValue = nil
                        
                        if #Library._config._flags[settings.flag] > 0 then
                            CurrentTargetValue = convertTableToString(Library._config._flags[settings.flag])
                        end

                        local selected = {}

                        if CurrentTargetValue then
                            for value in string.gmatch(CurrentTargetValue, "([^,]+)") do
                                local trimmedValue = value:match("^%s*(.-)%s*$")
                                
                                if trimmedValue ~= "Label" then
                                    table.insert(selected, trimmedValue)
                                end
                            end
                        else
                            for value in string.gmatch(CurrentOption.Text, "([^,]+)") do
                                local trimmedValue = value:match("^%s*(.-)%s*$")
                                
                                if trimmedValue ~= "Label" then
                                    table.insert(selected, trimmedValue)
                                end
                            end
                        end
                
                        local CurrentTextGet = convertStringToTable(CurrentOption.Text)

                        local optionSkibidi = "nil"
                        if typeof(option) ~= 'string' then
                            optionSkibidi = option.Name
                        else
                            optionSkibidi = option
                        end

                        local found = false
                        for i, v in pairs(CurrentTextGet) do
                            if v == optionSkibidi then
                                table.remove(CurrentTextGet, i)
                                break
                            end
                        end

                        CurrentOption.Text = table.concat(selected, ", ")
                        local OptionsChild = {}
                        for _, object in Options:GetChildren() do
                            if object.Name == "Option" then
                                table.insert(OptionsChild, object.Text)
                                if table.find(selected, object.Text) then
                                    object.TextTransparency = 0.2
                                else
                                    object.TextTransparency = 0.6
                                end
                            end
                        end

                        CurrentTargetValue = convertStringToTable(CurrentOption.Text)

                        for _, v in CurrentTargetValue do
                            if not table.find(OptionsChild, v) and table.find(selected, v) then
                                table.remove(selected, _)
                            end
                        end

                        CurrentOption.Text = table.concat(selected, ", ")
                
                        Library._config._flags[settings.flag] = convertStringToTable(CurrentOption.Text)
                    else
                        CurrentOption.Text = (typeof(option) == "string" and option) or option.Name
                        for _, object in Options:GetChildren() do
                            if object.Name == "Option" then
                                if object.Text == CurrentOption.Text then
                                    object.TextTransparency = 0.2
                                else
                                    object.TextTransparency = 0.6
                                end
                            end
                        end
                        Library._config._flags[settings.flag] = option
                    end
                
                    Config:save(game.GameId, Library._config)
                
                    settings.callback(option)
                end
                
                local CurrentDropSizeState = 0

                function DropdownManager:unfold_settings()
                    self._state = not self._state

                    if self._state then
                        ModuleManager._multiplier = ModuleManager._multiplier + self._size

                        CurrentDropSizeState = self._size

                        TweenService:Create(Module, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(241, 93 + ModuleManager._size + ModuleManager._multiplier)
                        }):Play()

                        TweenService:Create(Module.Options, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(241, ModuleManager._size + ModuleManager._multiplier)
                        }):Play()

                        TweenService:Create(Dropdown, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(207, 39 + self._size)
                        }):Play()

                        TweenService:Create(Box, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(207, 22 + self._size)
                        }):Play()

                        TweenService:Create(Arrow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Rotation = 180
                        }):Play()
                    else
                        ModuleManager._multiplier = ModuleManager._multiplier - self._size

                        CurrentDropSizeState = 0

                        TweenService:Create(Module, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(241, 93 + ModuleManager._size + ModuleManager._multiplier)
                        }):Play()

                        TweenService:Create(Module.Options, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(241, ModuleManager._size + ModuleManager._multiplier)
                        }):Play()

                        TweenService:Create(Dropdown, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(207, 39)
                        }):Play()

                        TweenService:Create(Box, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Size = UDim2.fromOffset(207, 22)
                        }):Play()

                        TweenService:Create(Arrow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {
                            Rotation = 0
                        }):Play()
                    end
                end

                if #settings.options > 0 then
                    DropdownManager._size = 3

                    for index, value in settings.options do
                        local Option = Instance.new('TextButton')
                        Option.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                        Option.Active = false
                        Option.TextTransparency = 0.6000000238418579
                        Option.AnchorPoint = Vector2.new(0, 0.5)
                        Option.TextSize = 10
                        Option.Size = UDim2.new(0, 186, 0, 16)
                        Option.TextColor3 = Color3.fromRGB(255,255,255)
                        Option.BorderColor3 = Color3.fromRGB(50, 50, 50)
                        Option.Text = (typeof(value) == "string" and value) or value.Name
                        Option.AutoButtonColor = false
                        Option.Name = 'Option'
                        Option.BackgroundTransparency = 1
                        Option.TextXAlignment = Enum.TextXAlignment.Left
                        Option.Selectable = false
                        Option.Position = UDim2.new(0.04999988153576851, 0, 0.34210526943206787, 0)
                        Option.BorderSizePixel = 0
                        Option.BackgroundColor3 = Color3.fromRGB(255,255,255)
                        Option.Parent = Options
                        
                        local UIGradient = Instance.new('UIGradient')
                        UIGradient.Transparency = NumberSequence.new{
                            NumberSequenceKeypoint.new(0, 0),
                            NumberSequenceKeypoint.new(0.704, 0),
                            NumberSequenceKeypoint.new(0.872, 0.36250001192092896),
                            NumberSequenceKeypoint.new(1, 1)
                        }
                        UIGradient.Parent = Option

                        Option.MouseButton1Click:Connect(function()
                            if not Library._config._flags[settings.flag] then
                                Library._config._flags[settings.flag] = {}
                            end

                            if settings.multi_dropdown then
                                if table.find(Library._config._flags[settings.flag], value) then
                                    Library:remove_table_value(Library._config._flags[settings.flag], value)
                                else
                                    table.insert(Library._config._flags[settings.flag], value)
                                end
                            end

                            DropdownManager:update(value)
                        end)
    
                        if index > settings.maximum_options then
                            continue
                        end
    
                        DropdownManager._size = DropdownManager._size + 16
                        Options.Size = UDim2.fromOffset(207, DropdownManager._size)
                    end
                end

                function DropdownManager:New(value)
                    Dropdown:Destroy()
                    value.OrderValue = Dropdown.LayoutOrder
                    ModuleManager._multiplier = ModuleManager._multiplier - CurrentDropSizeState
                    return ModuleManager:create_dropdown(value)
                end

                if Library:flag_type(settings.flag, 'string') then
                    DropdownManager:update(Library._config._flags[settings.flag])
                else
                    DropdownManager:update(settings.options[1])
                end
    
                Dropdown.MouseButton1Click:Connect(function()
                    DropdownManager:unfold_settings()
                end)

                return DropdownManager
            end

            function ModuleManager:create_feature(settings)

                local checked = false
                
                LayoutOrderModule = LayoutOrderModule + 1
            
                if self._size == 0 then
                    self._size = 11
                end
            
                self._size = self._size + 20
            
                if ModuleManager._state then
                    Module.Size = UDim2.fromOffset(241, 93 + self._size)
                end
            
                Options.Size = UDim2.fromOffset(241, self._size)
            
                local FeatureContainer = Instance.new("Frame")
                FeatureContainer.Size = UDim2.new(0, 207, 0, 16)
                FeatureContainer.BackgroundTransparency = 1
                FeatureContainer.Parent = Options
                FeatureContainer.LayoutOrder = LayoutOrderModule
            
                local UIListLayout = Instance.new("UIListLayout")
                UIListLayout.FillDirection = Enum.FillDirection.Horizontal
                UIListLayout.SortOrder = Enum.SortOrder.LayoutOrder
                UIListLayout.Parent = FeatureContainer
            
                local FeatureButton = Instance.new("TextButton")
                FeatureButton.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                FeatureButton.TextSize = 11
                FeatureButton.Size = UDim2.new(1, -35, 0, 16)
                FeatureButton.BackgroundColor3 = Color3.fromRGB(0,0,0)
                FeatureButton.TextColor3 = Color3.fromRGB(255,255,255)
                FeatureButton.Text = "    " .. settings.title or "    " .. "Feature"
                FeatureButton.AutoButtonColor = false
                FeatureButton.TextXAlignment = Enum.TextXAlignment.Left
                FeatureButton.TextTransparency = 0.2
                FeatureButton.Parent = FeatureContainer
            
                local RightContainer = Instance.new("Frame")
                RightContainer.Size = UDim2.new(0, 45, 0, 16)
                RightContainer.BackgroundTransparency = 1
                RightContainer.Parent = FeatureContainer
            
                local RightLayout = Instance.new("UIListLayout")
                RightLayout.Padding = UDim.new(0.1, 0)
                RightLayout.FillDirection = Enum.FillDirection.Horizontal
                RightLayout.HorizontalAlignment = Enum.HorizontalAlignment.Right
                RightLayout.SortOrder = Enum.SortOrder.LayoutOrder
                RightLayout.Parent = RightContainer
            
                local KeybindBox = Instance.new("TextLabel")
                KeybindBox.FontFace = Font.new('rbxasset://fonts/families/GothamSSm.json', Enum.FontWeight.SemiBold, Enum.FontStyle.Normal)
                KeybindBox.Size = UDim2.new(0, 15, 0, 15)
                KeybindBox.BackgroundColor3 = Color3.fromRGB(255,255,255)
                KeybindBox.TextColor3 = Color3.fromRGB(255,255,255)
                KeybindBox.TextSize = 11
                KeybindBox.BackgroundTransparency = 1
                KeybindBox.LayoutOrder = 2
                KeybindBox.Parent = RightContainer
            
                local KeybindButton = Instance.new("TextButton")
                KeybindButton.Size = UDim2.new(1, 0, 1, 0)
                KeybindButton.BackgroundTransparency = 1
                KeybindButton.TextTransparency = 1
                KeybindButton.Parent = KeybindBox

                local CheckboxCorner = Instance.new("UICorner", KeybindBox)
                CheckboxCorner.CornerRadius = UDim.new(0, 3)

                local UIStroke = Instance.new("UIStroke", KeybindBox)
                UIStroke.Color = Color3.fromRGB(255,255,255)
                UIStroke.Thickness = 1
                UIStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
            
                if not Library._config._flags then
                    Library._config._flags = {}
                end
            
                if not Library._config._flags[settings.flag] then
                    Library._config._flags[settings.flag] = {
                        checked = false,
                        BIND = settings.default or "Unknown"
                    }
                end
            
                checked = Library._config._flags[settings.flag].checked
                KeybindBox.Text = Library._config._flags[settings.flag].BIND

                if KeybindBox.Text == "Unknown" then
                    KeybindBox.Text = "..."
                end

                local UseF_Var = nil
            
                if not settings.disablecheck then
                    local Checkbox = Instance.new("TextButton")
                    Checkbox.Size = UDim2.new(0, 15, 0, 15)
                    Checkbox.BackgroundColor3 = checked and Color3.fromRGB(255,255,255) or Color3.fromRGB(0,0,0)
                    Checkbox.Text = ""
                    Checkbox.Parent = RightContainer
                    Checkbox.LayoutOrder = 1

                    local UIStroke = Instance.new("UIStroke", Checkbox)
                    UIStroke.Color = Color3.fromRGB(255,255,255)
                    UIStroke.Thickness = 1
                    UIStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
                
                    local CheckboxCorner = Instance.new("UICorner")
                    CheckboxCorner.CornerRadius = UDim.new(0, 3)
                    CheckboxCorner.Parent = Checkbox
            
                    local function toggleState()
                        checked = not checked
                        Checkbox.BackgroundColor3 = checked and Color3.fromRGB(255,255,255) or Color3.fromRGB(0,0,0)
                        Library._config._flags[settings.flag].checked = checked
                        Config:save(game.GameId, Library._config)
                        if settings.callback then
                            settings.callback(checked)
                        end
                    end

                    UseF_Var = toggleState
                
                    Checkbox.MouseButton1Click:Connect(toggleState)

                else

                    UseF_Var = function()
                        settings.button_callback()
                    end

                end
            
                KeybindButton.MouseButton1Click:Connect(function()
                    KeybindBox.Text = "..."
                    local inputConnection
                    inputConnection = game:GetService("UserInputService").InputBegan:Connect(function(input, gameProcessed)
                        if gameProcessed then return end
                        if input.UserInputType == Enum.UserInputType.Keyboard then
                            local newKey = input.KeyCode.Name
                            Library._config._flags[settings.flag].BIND = newKey
                            if newKey ~= "Unknown" then
                                KeybindBox.Text = newKey
                            end
                            Config:save(game.GameId, Library._config)
                            inputConnection:Disconnect()
                        elseif input.UserInputType == Enum.UserInputType.MouseButton3 then
                            Library._config._flags[settings.flag].BIND = "Unknown"
                            KeybindBox.Text = "..."
                            Config:save(game.GameId, Library._config)
                            inputConnection:Disconnect()
                        end
                    end)
                    Connections["keybind_input_" .. settings.flag] = inputConnection
                end)
            
                local keyPressConnection
                keyPressConnection = game:GetService("UserInputService").InputBegan:Connect(function(input, gameProcessed)
                    if gameProcessed then return end
                    if input.UserInputType == Enum.UserInputType.Keyboard then
                        if input.KeyCode.Name == Library._config._flags[settings.flag].BIND then
                            UseF_Var()
                        end
                    end
                end)
                Connections["keybind_press_" .. settings.flag] = keyPressConnection
            
                FeatureButton.MouseButton1Click:Connect(function()
                    if settings.button_callback then
                        settings.button_callback()
                    end
                end)

                if not settings.disablecheck then
                    settings.callback(checked)
                end
            
                return FeatureContainer
            end                    

            return ModuleManager
        end

        return TabManager
    end

    Connections['library_visiblity'] = UserInputService.InputBegan:Connect(function(input, process)
        if input.KeyCode ~= Enum.KeyCode.Insert then
            return
        end

        self._ui_open = not self._ui_open
        self:change_visiblity(self._ui_open)
    end)

    self._ui.Container.Handler.Minimize.MouseButton1Click:Connect(function()
        self._ui_open = not self._ui_open
        self:change_visiblity(self._ui_open)
    end)

    return self
end

local main = Library.new()
local parry = main:create_tab("Auto Parry", "rbxassetid://76499042599127")
local spam = main:create_tab("Spam", "rbxassetid://132243429647479")

repeat
	task.wait()
until game:IsLoaded()

local Players = game:GetService("Players")
local Player = Players.LocalPlayer
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Tornado_Time = tick()
local UserInputService = game:GetService("UserInputService")
local Last_Input = UserInputService:GetLastInputType()
local Debris = game:GetService("Debris")
local RunService = game:GetService("RunService")
local Vector2_Mouse_Location = nil
local Grab_Parry = nil
local Remotes = {}
local Parry_Key = nil
local Speed_Divisor_Multiplier = 0.9
local LobbyAP_Speed_Divisor_Multiplier = 0.9
local firstParryFired = false
local ParryThreshold = 1.5
local firstParryType = "F_Key"
local Previous_Positions = {}
local VirtualInputManager = game:GetService("VirtualInputManager")
local VirtualInputService = game:GetService("VirtualInputManager")
local parryCooldown = 0.0  -- tuned: aggressive (0.0)
local lastParryTime = 0
local GuiService = game:GetService("GuiService")

local function updateNavigation(guiObject)
	GuiService.SelectedObject = guiObject
end

local function performFirstPress(parryType)
	if parryType == "F_Key" then
		VirtualInputService:SendKeyEvent(true, Enum.KeyCode.F, false, nil)
	elseif parryType == "Left_Click" then
		VirtualInputManager:SendMouseButtonEvent(0, 0, 0, true, game, 0)
	elseif parryType == "Navigation" then
		local button = Players.LocalPlayer.PlayerGui.Hotbar.Block
		updateNavigation(button)
		VirtualInputManager:SendKeyEvent(true, Enum.KeyCode.Return, false, game)
		VirtualInputManager:SendKeyEvent(false, Enum.KeyCode.Return, false, game)
		task.wait(0.0)
		updateNavigation(nil)
	end
end

if not LPH_OBFUSCATED then
	function LPH_JIT(Function)
		return Function
	end
	function LPH_JIT_MAX(Function)
		return Function
	end
	function LPH_NO_VIRTUALIZE(Function)
		return Function
	end
end

local PrivateKey = nil
local PropertyChangeOrder = {}
local HashOne
local HashTwo
local HashThree

LPH_NO_VIRTUALIZE(function()
	for Index, Value in next, getgc() do
		if
			rawequal(typeof(Value), "function")
			and islclosure(Value)
			and getrenv().debug.info(Value, "s"):find("SwordsController")
		then
			if rawequal(getrenv().debug.info(Value, "l"), 263) then
				HashOne = getconstant(Value, 62)
				HashTwo = getconstant(Value, 64)
				HashThree = getconstant(Value, 65)
			end
		end
	end
end)()

LPH_NO_VIRTUALIZE(function()
	for Index, Object in next, game:GetDescendants() do
		if Object:IsA("RemoteEvent") and string.find(Object.Name, "\n") then
			Object.Changed:Once(function()
				table.insert(PropertyChangeOrder, Object)
			end)
		end
	end
end)()
local ShouldPlayerJump = PropertyChangeOrder[1]
local MainRemote = PropertyChangeOrder[2]
local GetOpponentPosition = PropertyChangeOrder[3]
local Remotes = {}
local Parry_Key = nil
local activeMethod = "Remote"

local revertedRemotes = {}
local originalMetatables = {}

local function isValidRemoteArgs(args)
	return #args == 7 and
		   type(args[2]) == "string" and  
		   type(args[3]) == "number" and 
		   typeof(args[4]) == "CFrame" and 
		   type(args[5]) == "table" and  
		   type(args[6]) == "table" and 
		   type(args[7]) == "boolean"
end

local function hookRemote(remote)
	if not revertedRemotes[remote] then
		if not originalMetatables[getmetatable(remote)] then
			originalMetatables[getmetatable(remote)] = true

			local meta = getrawmetatable(remote)
			setreadonly(meta, false)

			local oldIndex = meta.__index
			meta.__index = function(self, key)
				if (key == "FireServer" and self:IsA("RemoteEvent")) or (key == "InvokeServer" and self:IsA("RemoteFunction")) then
					return function(_, ...)
						local args = {...}
						if isValidRemoteArgs(args) then
							if not revertedRemotes[self] then
								revertedRemotes[self] = args

								Remotes[self] = args[1]
								Parry_Key = args[2]

								local remoteType = self:IsA("RemoteEvent") and "RemoteEvent" or "RemoteFunction"
								local remoteData = {
									RemoteName = self.Name,
									RemoteType = remoteType,
									Args = args
								}
								setclipboard(game:GetService("HttpService"):JSONEncode(remoteData))

								print("✅ Remote copied to clipboard! (" .. self.Name .. ")")
							end
						end
						return oldIndex(self, key)(_, unpack(args))
					end
				end
				return oldIndex(self, key)
			end

			setreadonly(meta, true)
		end
	end
end

local function restoreRemotes()
	for remote, _ in pairs(revertedRemotes) do
		if originalMetatables[getmetatable(remote)] then
			local meta = getrawmetatable(remote)
			setreadonly(meta, false)
			meta.__index = nil
			setreadonly(meta, true)
		end
	end
	revertedRemotes = {}
	print("Remotes restored.")
end

for _, remote in pairs(game.ReplicatedStorage:GetChildren()) do
	if remote:IsA("RemoteEvent") or remote:IsA("RemoteFunction") then
		hookRemote(remote)
	end
end

game.ReplicatedStorage.ChildAdded:Connect(function(child)
	if child:IsA("RemoteEvent") or child:IsA("RemoteFunction") then
		hookRemote(child)
	end
end)

local Key = Parry_Key

local function Parry(...)
	ShouldPlayerJump:FireServer(HashOne, Parry_Key, ...)
	MainRemote:FireServer(HashTwo, Parry_Key, ...)
	GetOpponentPosition:FireServer(HashThree, Parry_Key, ...)
end

local function getFunction(t)
	t = t or {}
	local functions = {}
	local function findMatches()
		Setthreadidentity(6)
		for i, v in getgc() do
			if type(v) == "function" and islclosure(v) then
				local match = true
				local info = getinfo(v)
				if t.scriptName and (not tostring(getfenv(v).script):find(t.scriptName)) then
					match = false
				end
				if t.name and info.name ~= t.name then
					match = false
				end
				if t.line and info.currentline ~= t.line then
					match = false
				end
				if t.upvalueCount and #getupvalues(v) ~= t.upvalueCount then
					match = false
				end
				if t.constantCount and #getconstants(v) ~= t.constantsCount then
					match = false
				end
				if match then
					table.insert(functions, v)
				end
			end
		end
		setthreadidentity(8)
	end
	findMatches()
	if #functions == 0 then
		while task.wait(1) and #functions == 0 do
			findMatches()
		end
	end
	if #functions == 1 then
		return functions[1]
	end
end

getgenv().skinChanger = false
getgenv().swordModel = ""
getgenv().swordAnimations = ""
getgenv().swordFX = ""

local print = function() end

if getgenv().updateSword and getgenv().skinChanger then
	getgenv().updateSword()
	return
end

local function getTable(t)
	t = t or {}
	local tables = {}
	local function findMatches()
		for i, v in getgc(true) do
			if type(v) == "table" then
				local match = true
				if t.highEntropyTableIndex and (not rawget(v, t.highEntropyTableIndex)) then
					match = false
				end
				if match then
					table.insert(tables, v)
				end
			end
		end
	end
	findMatches()
	if #tables == 0 then
		while task.wait(1) and #tables == 0 do
			findMatches()
		end
	end
	if #tables == 1 then
		return tables[1]
	end
end

local plrs = game:GetService("Players")
local plr = plrs.LocalPlayer
local rs = game:GetService("ReplicatedStorage")
local swordInstancesInstance =
	rs:WaitForChild("Shared", 9e9):WaitForChild("ReplicatedInstances", 9e9):WaitForChild("Swords", 9e9)
local swordInstances = require(swordInstancesInstance)
local swordsController

while task.wait() and not swordsController do
	for i, v in getconnections(rs.Remotes.FireSwordInfo.OnClientEvent) do
		if v.Function and islclosure(v.Function) then
			local upvalues = getupvalues(v.Function)
			if #upvalues == 1 and type(upvalues[1]) == "table" then
				swordsController = upvalues[1]
				break
			end
		end
	end
end

function getSlashName(swordName)
	local slashName = swordInstances:GetSword(swordName)
	return (slashName and slashName.SlashName) or "SlashEffect"
end

function setSword()
	if not getgenv().skinChanger then
		return
	end
	setupvalue(rawget(swordInstances, "EquipSwordTo"), 2, false)
	swordInstances:EquipSwordTo(plr.Character, getgenv().swordModel)
	swordsController:SetSword(getgenv().swordAnimations)
end

local playParryFunc
local parrySuccessAllConnection

while task.wait() and not parrySuccessAllConnection do
	for i, v in getconnections(rs.Remotes.ParrySuccessAll.OnClientEvent) do
		if v.Function and getinfo(v.Function).name == "parrySuccessAll" then
			parrySuccessAllConnection = v
			playParryFunc = v.Function
			v:Disable()
		end
	end
end

local parrySuccessClientConnection

while task.wait() and not parrySuccessClientConnection do
	for i, v in getconnections(rs.Remotes.ParrySuccessClient.Event) do
		if v.Function and getinfo(v.Function).name == "parrySuccessAll" then
			parrySuccessClientConnection = v
			v:Disable()
		end
	end
end

getgenv().slashName = getSlashName(getgenv().swordFX)

local lastOtherParryTimestamp = 0
local clashConnections = {}

rs.Remotes.ParrySuccessAll.OnClientEvent:Connect(function(...)
	setthreadidentity(2)
	local args = { ... }
	if tostring(args[4]) ~= plr.Name then
		lastOtherParryTimestamp = tick()
	elseif getgenv().skinChanger then
		args[1] = getgenv().slashName
		args[3] = getgenv().swordFX
	end
	return playParryFunc(unpack(args))
end)

table.insert(clashConnections, getconnections(rs.Remotes.ParrySuccessAll.OnClientEvent)[1])

getgenv().updateSword = function()
	getgenv().slashName = getSlashName(getgenv().swordFX)
	setSword()
end

task.spawn(function()
	while task.wait(1) do
		if getgenv().skinChanger then
			local char = plr.Character or plr.CharacterAdded:Wait()
			if plr:GetAttribute("CurrentlyEquippedSword") ~= getgenv().swordModel then
				setSword()
			end
			if char and (not char:FindFirstChild(getgenv().swordModel)) then
				setSword()
			end
			for _, v in (char and char:GetChildren()) or {} do
				if v:IsA("Model") and v.Name ~= getgenv().swordModel then
					v:Destroy()
				end
				task.wait()
			end
		end
	end
end)

local Parries = 0

function create_animation(object, info, value)
	local animation = game:GetService("TweenService"):Create(object, info, value)
	animation:Play()
	task.wait(info.Time)
	Debris:AddItem(animation, 0)
	animation:Destroy()
	animation = nil
end

local Animation = {}
Animation.storage = {}
Animation.current = nil
Animation.track = nil

for _, v in pairs(game:GetService("ReplicatedStorage").Misc.Emotes:GetChildren()) do
	if v:IsA("Animation") and v:GetAttribute("EmoteName") then
		local Emote_Name = v:GetAttribute("EmoteName")
		Animation.storage[Emote_Name] = v
	end
end

local Emotes_Data = {}
for Object in pairs(Animation.storage) do
	table.insert(Emotes_Data, Object)
end
table.sort(Emotes_Data)

local Auto_Parry = {}

function Auto_Parry.Parry_Animation()
	local Parry_Animation = game:GetService("ReplicatedStorage").Shared.SwordAPI.Collection.Default
		:FindFirstChild("GrabParry")
	local Current_Sword = Player.Character:GetAttribute("CurrentlyEquippedSword")
	if not Current_Sword then
		return
	end
	if not Parry_Animation then
		return
	end
	local Sword_Data = game:GetService("ReplicatedStorage").Shared.ReplicatedInstances.Swords.GetSword
		:Invoke(Current_Sword)
	if not Sword_Data or not Sword_Data["AnimationType"] then
		return
	end
	for _, object in pairs(game:GetService("ReplicatedStorage").Shared.SwordAPI.Collection:GetChildren()) do
		if object.Name == Sword_Data["AnimationType"] then
			if object:FindFirstChild("GrabParry") or object:FindFirstChild("Grab") then
				local sword_animation_type = "GrabParry"
				if object:FindFirstChild("Grab") then
					sword_animation_type = "Grab"
				end
				Parry_Animation = object[sword_animation_type]
			end
		end
	end
	Grab_Parry = Player.Character.Humanoid.Animator:LoadAnimation(Parry_Animation)
	Grab_Parry:Play()
end

function Auto_Parry.Play_Animation(v)
	local Animations = Animation.storage[v]
	if not Animations then
		return false
	end
	local Animator = Player.Character.Humanoid.Animator
	if Animation.track then
		Animation.track:Stop()
	end
	Animation.track = Animator:LoadAnimation(Animations)
	Animation.track:Play()
	Animation.current = v
end

function Auto_Parry.Get_Balls()
	local Balls = {}
	for _, Instance in pairs(workspace.Balls:GetChildren()) do
		if Instance:GetAttribute("realBall") then
			Instance.CanCollide = false
			table.insert(Balls, Instance)
		end
	end
	return Balls
end

function Auto_Parry.Get_Ball()
	for _, Instance in pairs(workspace.Balls:GetChildren()) do
		if Instance:GetAttribute("realBall") then
			Instance.CanCollide = false
			return Instance
		end
	end
end

function Auto_Parry.Lobby_Balls()
	for _, Instance in pairs(workspace.TrainingBalls:GetChildren()) do
		if Instance:GetAttribute("realBall") then
			return Instance
		end
	end
end

local Closest_Entity = nil

function Auto_Parry.Closest_Player()
	local Max_Distance = math.huge
	local Found_Entity = nil
	for _, Entity in pairs(workspace.Alive:GetChildren()) do
		if tostring(Entity) ~= tostring(Player) then
			if Entity.PrimaryPart then
				local Distance = Player:DistanceFromCharacter(Entity.PrimaryPart.Position)
				if Distance < Max_Distance then
					Max_Distance = Distance
					Found_Entity = Entity
				end
			end
		end
	end
	Closest_Entity = Found_Entity
	return Found_Entity
end

function Auto_Parry:Get_Entity_Properties()
	Auto_Parry.Closest_Player()
	if not Closest_Entity then
		return false
	end
	local Entity_Velocity = Closest_Entity.PrimaryPart.Velocity
	local Entity_Direction = (Player.Character.PrimaryPart.Position - Closest_Entity.PrimaryPart.Position).Unit
	local Entity_Distance = (Player.Character.PrimaryPart.Position - Closest_Entity.PrimaryPart.Position).Magnitude
	return {
		Velocity = Entity_Velocity,
		Direction = Entity_Direction,
		Distance = Entity_Distance,
	}
end

local isMobile = UserInputService.TouchEnabled and not UserInputService.MouseEnabled

function Auto_Parry.Parry_Data(Parry_Type)
	Auto_Parry.Closest_Player()
	local Events = {}
	local Camera = workspace.CurrentCamera
	local Vector2_Mouse_Location
	if
		Last_Input == Enum.UserInputType.MouseButton1
		or (Enum.UserInputType.MouseButton2 or Last_Input == Enum.UserInputType.Keyboard)
	then
		local Mouse_Location = UserInputService:GetMouseLocation()
		Vector2_Mouse_Location = { Mouse_Location.X, Mouse_Location.Y }
	else
		Vector2_Mouse_Location = { Camera.ViewportSize.X / 2, Camera.ViewportSize.Y / 2 }
	end
	if isMobile then
		Vector2_Mouse_Location = { Camera.ViewportSize.X / 2, Camera.ViewportSize.Y / 2 }
	end
	local Players_Screen_Positions = {}
	for _, v in pairs(workspace.Alive:GetChildren()) do
		if v ~= Player.Character then
			local worldPos = v.PrimaryPart.Position
			local screenPos, isOnScreen = Camera:WorldToScreenPoint(worldPos)
			if isOnScreen then
				Players_Screen_Positions[v] = Vector2.new(screenPos.X, screenPos.Y)
			end
			Events[tostring(v)] = screenPos
		end
	end
	if Parry_Type == "Camera" then
		return { 0, Camera.CFrame, Events, Vector2_Mouse_Location }
	end
	if Parry_Type == "Backwards" then
		local Backwards_Direction = Camera.CFrame.LookVector * -10000
		Backwards_Direction = Vector3.new(Backwards_Direction.X, 0, Backwards_Direction.Z)
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position + Backwards_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "Straight" then
		local Aimed_Player = nil
		local Closest_Distance = math.huge
		local Mouse_Vector = Vector2.new(Vector2_Mouse_Location[1], Vector2_Mouse_Location[2])
		for _, v in pairs(workspace.Alive:GetChildren()) do
			if v ~= Player.Character then
				local worldPos = v.PrimaryPart.Position
				local screenPos, isOnScreen = Camera:WorldToScreenPoint(worldPos)
				if isOnScreen then
					local playerScreenPos = Vector2.new(screenPos.X, screenPos.Y)
					local distance = (Mouse_Vector - playerScreenPos).Magnitude
					if distance < Closest_Distance then
						Closest_Distance = distance
						Aimed_Player = v
					end
				end
			end
		end
		if Aimed_Player then
			return {
				0,
				CFrame.new(Player.Character.PrimaryPart.Position, Aimed_Player.PrimaryPart.Position),
				Events,
				Vector2_Mouse_Location,
			}
		else
			return {
				0,
				CFrame.new(Player.Character.PrimaryPart.Position, Closest_Entity.PrimaryPart.Position),
				Events,
				Vector2_Mouse_Location,
			}
		end
	end
	if Parry_Type == "Random" then
		return {
			0,
			CFrame.new(
				Camera.CFrame.Position,
				Vector3.new(math.random(-4000, 4000), math.random(-4000, 4000), math.random(-4000, 4000))
			),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "High" then
		local High_Direction = Camera.CFrame.UpVector * 10000
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position + High_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "Low" then
		local Low_Direction = Vector3.new(0, -1, 0) * 10000
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position + Low_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "Slowball" then
		local Slowball_Direction = Vector3.new(0, -1, 0) * 99999
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position + Slowball_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "Left" then
		local Left_Direction = Camera.CFrame.RightVector * 10000
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position - Left_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "Right" then
		local Right_Direction = Camera.CFrame.RightVector * 10000
		return {
			0,
			CFrame.new(Camera.CFrame.Position, Camera.CFrame.Position + Right_Direction),
			Events,
			Vector2_Mouse_Location,
		}
	end
	if Parry_Type == "RandomTarget" then
		local candidates = {}
		for _, v in pairs(workspace.Alive:GetChildren()) do
			if v ~= Player.Character and v.PrimaryPart then
				local screenPos, isOnScreen = Camera:WorldToScreenPoint(v.PrimaryPart.Position)
				if isOnScreen then
					table.insert(candidates, {
						character = v,
						screenXY = { screenPos.X, screenPos.Y },
					})
				end
			end
		end
		if #candidates > 0 then
			local pick = candidates[math.random(1, #candidates)]
			local lookCFrame = CFrame.new(Player.Character.PrimaryPart.Position, pick.character.PrimaryPart.Position)
			return { 0, lookCFrame, Events, pick.screenXY }
		else
			return { 0, Camera.CFrame, Events, { Camera.ViewportSize.X / 2, Camera.ViewportSize.Y / 2 } }
		end
	end
	return Parry_Type
end

function Auto_Parry.Parry(Parry_Type)
	if tick() - lastParryTime < parryCooldown then
		return false
	end
	lastParryTime = tick()
	local presses = isSpam and spamSpeed or 1
	if activeMethod == "F Key" then
		for i = 1, presses do
			VirtualInputService:SendKeyEvent(true, Enum.KeyCode.F, false, game)
			VirtualInputService:SendKeyEvent(false, Enum.KeyCode.F, false, game)
		end
	else
		local foundFake = false
		for _, Args in pairs(Remotes) do
			if Args == "PARRY_HASH_FAKE_1" or Args == "_G" then
				foundFake = true
				break
			end
		end
		local Parry_Data = Auto_Parry.Parry_Data(Parry_Type)
		for i = 1, presses do
			for Remote, Args in pairs(Remotes) do
				local Hash
				if foundFake then
					Hash = nil
				else
					Hash = Args
				end
				Remote:FireServer(Hash, Parry_Key, Parry_Data[1], Parry_Data[2], Parry_Data[3], Parry_Data[4])
			end
		end
	end
	if Parries > 7 then
		return false
	end
	Parries = Parries + 1
	task.delay(0.5, function()
		if Parries > 0 then
			Parries = Parries - 1
		end
	end)
end

local Lerp_Radians = 0
local Last_Warping = tick()

function Auto_Parry.Linear_Interpolation(a, b, time_volume)
	return a + (b - a) * time_volume
end

local Previous_Velocity = {}
local Curving = tick()
local Runtime = workspace.Runtime

function Auto_Parry.Is_Curved()
	local Ball = Auto_Parry.Get_Ball()
	if not Ball then
		return false
	end
	local Zoomies = Ball:FindFirstChild("zoomies")
	if not Zoomies then
		return false
	end
	local Ping = game:GetService("Stats").Network.ServerStatsItem["Data Ping"]:GetValue()
	local Velocity = Zoomies.VectorVelocity
	local Ball_Direction = Velocity.Unit
	local playerPos = Player.Character.PrimaryPart.Position
	local ballPos = Ball.Position
	local Direction = (playerPos - ballPos).Unit
	local Dot = Direction:Dot(Ball_Direction)
	local Speed = Velocity.Magnitude
	local Speed_Threshold = math.min(Speed / 100, 40)
	local Angle_Threshold = 40 * math.max(Dot, 0)
	local Distance = (playerPos - ballPos).Magnitude
	local Reach_Time = Distance / Speed - (Ping / 1000)
	local Ball_Distance_Threshold = 15 - math.min(Distance / 1000, 15) + Speed_Threshold
	table.insert(Previous_Velocity, Velocity)
	if #Previous_Velocity > 4 then
		table.remove(Previous_Velocity, 1)
	end
	if Ball:FindFirstChild("AeroDynamicSlashVFX") then
		Debris:AddItem(Ball.AeroDynamicSlashVFX, 0)
		Tornado_Time = tick()
	end
	if Runtime:FindFirstChild("Tornado") then
		if (tick() - Tornado_Time) < ((Runtime.Tornado:GetAttribute("TornadoTime") or 1) + 0.314159) then
			return true
		end
	end
	local Enough_Speed = Speed > 160
	if Enough_Speed and Reach_Time > (Ping / 10 + 0.03) then
		if Speed < 300 then
			Ball_Distance_Threshold = math.max(Ball_Distance_Threshold - 15, 15)
		elseif Speed <= 600 then
			Ball_Distance_Threshold = math.max(Ball_Distance_Threshold - 17, 17)
		elseif Speed <= 1000 then
			Ball_Distance_Threshold = math.max(Ball_Distance_Threshold - 19, 19)
		else
			Ball_Distance_Threshold = math.max(Ball_Distance_Threshold - 20, 20)
		end
	end
	if Distance < Ball_Distance_Threshold then
		return false
	end
	local adjustedReachTime = Reach_Time + 0.03
	if Speed < 300 then
		if (tick() - Curving) < (adjustedReachTime / 1.2) then
			return true
		end
	elseif Speed < 450 then
		if (tick() - Curving) < (adjustedReachTime / 1.21) then
			return true
		end
	elseif Speed < 600 then
		if (tick() - Curving) < (adjustedReachTime / 1.335) then
			return true
		end
	else
		if (tick() - Curving) < (adjustedReachTime / 1.5) then
			return true
		end
	end
	local Dot_Threshold = (0 - Ping / 1000)
	local Direction_Difference = (Ball_Direction - Velocity.Unit)
	local Direction_Similarity = Direction:Dot(Direction_Difference.Unit)
	local Dot_Difference = Dot - Direction_Similarity
	if Dot_Difference < Dot_Threshold then
		return true
	end
	local Clamped_Dot = math.clamp(Dot, -1, 1)
	local Radians = math.deg(math.asin(Clamped_Dot))
	Lerp_Radians = Auto_Parry.Linear_Interpolation(Lerp_Radians, Radians, 0.8)
	if Speed < 300 then
		if Lerp_Radians < 0.02 then
			Last_Warping = tick()
		end
		if (tick() - Last_Warping) < (adjustedReachTime / 1.19) then
			return true
		end
	else
		if Lerp_Radians < 0.018 then
			Last_Warping = tick()
		end
		if (tick() - Last_Warping) < (adjustedReachTime / 1.5) then
			return true
		end
	end
	if #Previous_Velocity == 4 then
		for i = 1, 2 do
			local prevDir = (Ball_Direction - Previous_Velocity[i].Unit).Unit
			local prevDot = Direction:Dot(prevDir)
			if (Dot - prevDot) < Dot_Threshold then
				return true
			end
		end
	end
	local backwardsCurveDetected = false
	local backwardsAngleThreshold = 60
	local horizDirection = Vector3.new(playerPos.X - ballPos.X, 0, playerPos.Z - ballPos.Z)
	if horizDirection.Magnitude > 0 then
		horizDirection = horizDirection.Unit
	end
	local awayFromPlayer = -horizDirection
	local horizBallDir = Vector3.new(Ball_Direction.X, 0, Ball_Direction.Z)
	if horizBallDir.Magnitude > 0 then
		horizBallDir = horizBallDir.Unit
		local backwardsAngle = math.deg(math.acos(math.clamp(awayFromPlayer:Dot(horizBallDir), -1, 1)))
		if backwardsAngle < backwardsAngleThreshold then
			backwardsCurveDetected = true
		end
	end
	return (Dot < Dot_Threshold) or backwardsCurveDetected
end

function Auto_Parry:Get_Ball_Properties()
	local Ball = Auto_Parry.Get_Ball()
	local Ball_Velocity = Vector3.zero
	local Ball_Origin = Ball
	local Ball_Direction = (Player.Character.PrimaryPart.Position - Ball_Origin.Position).Unit
	local Ball_Distance = (Player.Character.PrimaryPart.Position - Ball.Position).Magnitude
	local Ball_Dot = Ball_Direction:Dot(Ball_Velocity.Unit)
	return {
		Velocity = Ball_Velocity,
		Direction = Ball_Direction,
		Distance = Ball_Distance,
		Dot = Ball_Dot,
	}
end

function Auto_Parry.Spam_Service(self)
	local Ball = Auto_Parry.Get_Ball()
	local Entity = Auto_Parry.Closest_Player()
	if not Ball then
		return false
	end
	if not Entity or not Entity.PrimaryPart then
		return false
	end
	local Spam_Accuracy = 0
	local Velocity = Ball.AssemblyLinearVelocity
	local Speed = Velocity.Magnitude
	local Direction = (Player.Character.PrimaryPart.Position - Ball.Position).Unit
	local Dot = Direction:Dot(Velocity.Unit)
	local Target_Position = Entity.PrimaryPart.Position
	local Target_Distance = Player:DistanceFromCharacter(Target_Position)
	local Movement_Factor = 1
	local MoveDir = Player.Character.Humanoid.MoveDirection
	local TargetDir = (Target_Position - Player.Character.PrimaryPart.Position).Unit
	local TargetMoveDir = Entity.Humanoid.MoveDirection
	_G.Last_Close_Contact = _G.Last_Close_Contact or 0
	_G.In_Close_Contact = _G.In_Close_Contact or false
	local now = tick()
	if Target_Distance <= 3 then
		_G.In_Close_Contact = true
	end
	if _G.In_Close_Contact and Target_Distance > 3.3 then
		_G.In_Close_Contact = false
		_G.Last_Close_Contact = now
	end
	local can_use_div10 = (not _G.In_Close_Contact) and ((now - _G.Last_Close_Contact) >= 1.5)
	if can_use_div10 and MoveDir.Magnitude > 0.2 and MoveDir:Dot(TargetDir) < -0.4 then
		Movement_Factor = 10
	end
	if can_use_div10 and TargetMoveDir.Magnitude > 0.2 and TargetMoveDir:Dot(-TargetDir) < -0.4 then
		Movement_Factor = 10
	end
	local Maximum_Spam_Distance = self.Ping * 0.7 + math.min(Speed / (Movement_Factor * 1.2), 80)
	if self.Entity_Properties.Distance > Maximum_Spam_Distance then
		return Spam_Accuracy
	end
	if self.Ball_Properties.Distance > Maximum_Spam_Distance then
		return Spam_Accuracy
	end
	if Target_Distance > Maximum_Spam_Distance then
		return Spam_Accuracy
	end
	local Dot_Reduction = math.clamp(-Dot, 0, 1)
	local Dot_Impact = math.clamp(Dot_Reduction * (Speed / 40), 0, 4)
	Spam_Accuracy = Maximum_Spam_Distance - Dot_Impact
	return Spam_Accuracy
end

local Connections_Manager = {}
local Selected_Parry_Type = nil
local Parried = false
local Last_Parry = 0
local deathshit = false

ReplicatedStorage.Remotes.DeathBall.OnClientEvent:Connect(function(c, d)
	if d then
		deathshit = true
	else
		deathshit = false
	end
end)

local Infinity = false

ReplicatedStorage.Remotes.InfinityBall.OnClientEvent:Connect(function(a, b)
	if b then
		Infinity = true
	else
		Infinity = false
	end
end)

local timehole = false

ReplicatedStorage.Remotes.TimeHoleHoldBall.OnClientEvent:Connect(function(e, f)
	if f then
		timehole = true
	else
		timehole = false
	end
end)

local AutoParry = true
local Balls = workspace:WaitForChild("Balls")
local CurrentBall = nil
local InputTask = nil
local Cooldown = 0
local RunTime = workspace:FindFirstChild("Runtime")

local function GetBall()
	for _, Ball in ipairs(Balls:GetChildren()) do
		if Ball:FindFirstChild("ff") then
			return Ball
		end
	end
	return nil
end

local function SpamInput(Label)
	if InputTask then
		return
	end
	InputTask = task.spawn(function()
		while AutoParry do
			Auto_Parry.Parry(Selected_Parry_Type)
			task.wait(Cooldown)
		end
		InputTask = nil
	end)
end

Balls.ChildAdded:Connect(function(Value)
	Value.ChildAdded:Connect(function(Child)
		if getgenv().SlashOfFuryDetection and Child.Name == "ComboCounter" then
			local Sof_Label = Child:FindFirstChildOfClass("TextLabel")
			if Sof_Label then
				repeat
					local Slashes_Counter = tonumber(Sof_Label.Text)
					if Slashes_Counter and Slashes_Counter < 32 then
						Auto_Parry.Parry(Selected_Parry_Type)
					end
					task.wait()
				until not Sof_Label.Parent or not Sof_Label
			end
		end
	end)
end)

local Players = game:GetService("Players")
local player10239123 = Players.LocalPlayer
local RunService = game:GetService("RunService")

if not player10239123 then
	return
end

RunTime.ChildAdded:Connect(function(Object)
	local Name = Object.Name
	if getgenv().PhantomV2Detection then
		if Name == "maxTransmission" or Name == "transmissionpart" then
			local Weld = Object:FindFirstChildWhichIsA("WeldConstraint")
			if Weld then
				local Character = player10239123.Character or player10239123.CharacterAdded:Wait()
				if Character and Weld.Part1 == Character.HumanoidRootPart then
					CurrentBall = GetBall()
					Weld:Destroy()
					if CurrentBall then
						local FocusConnection
						FocusConnection = RunService.RenderStepped:Connect(function()
							local Highlighted = CurrentBall:GetAttribute("highlighted")
							if Highlighted == true then
								game.Players.LocalPlayer.Character.Humanoid.WalkSpeed = 36
								local HumanoidRootPart = Character:FindFirstChild("HumanoidRootPart")
								if HumanoidRootPart then
									local PlayerPosition = HumanoidRootPart.Position
									local BallPosition = CurrentBall.Position
									local PlayerToBall = (BallPosition - PlayerPosition).Unit
									game.Players.LocalPlayer.Character.Humanoid:Move(PlayerToBall, false)
								end
							elseif Highlighted == false then
								FocusConnection:Disconnect()
								game.Players.LocalPlayer.Character.Humanoid.WalkSpeed = 10
								game.Players.LocalPlayer.Character.Humanoid:Move(Vector3.new(0, 0, 0), false)
								task.delay(3, function()
									game.Players.LocalPlayer.Character.Humanoid.WalkSpeed = 36
								end)
								CurrentBall = nil
							end
						end)
						task.delay(3, function()
							if FocusConnection and FocusConnection.Connected then
								FocusConnection:Disconnect()
								game.Players.LocalPlayer.Character.Humanoid:Move(Vector3.new(0, 0, 0), false)
								game.Players.LocalPlayer.Character.Humanoid.WalkSpeed = 36
								CurrentBall = nil
							end
						end)
					end
				end
			end
		end
	end
end)

local player11 = game.Players.LocalPlayer
local PlayerGui = player11:WaitForChild("PlayerGui")
local playerGui = player11:WaitForChild("PlayerGui")
local Hotbar = PlayerGui:WaitForChild("Hotbar")
local ParryCD = playerGui.Hotbar.Block.UIGradient
local AbilityCD = playerGui.Hotbar.Ability.UIGradient

local function isCooldownInEffect1(uigradient)
	return uigradient.Offset.Y < 0.4
end

local function isCooldownInEffect2(uigradient)
	return uigradient.Offset.Y == 0.5
end

local function cooldownProtection()
	if isCooldownInEffect1(ParryCD) then
		game:GetService("ReplicatedStorage").Remotes.AbilityButtonPress:Fire()
		return true
	end
	return false
end

local function AutoAbility()
	if isCooldownInEffect2(AbilityCD) then
		if
			Player.Character.Abilities["Raging Deflection"].Enabled
			or Player.Character.Abilities["Rapture"].Enabled
			or Player.Character.Abilities["Calming Deflection"].Enabled
			or Player.Character.Abilities["Aerodynamic Slash"].Enabled
			or Player.Character.Abilities["Fracture"].Enabled
			or Player.Character.Abilities["Death Slash"].Enabled
		then
			Parried = true
			game:GetService("ReplicatedStorage").Remotes.AbilityButtonPress:Fire()
			task.wait(2.432)
			game:GetService("ReplicatedStorage")
				:WaitForChild("Remotes")
				:WaitForChild("DeathSlashShootActivation")
				:FireServer(true)
			return true
		end
	end
	return false
end

getgenv().firstParryFired = false
getgenv().firstParryType = "F_Key"
local module = parry:create_module({
	title = "Auto Parry",
	flag = "Auto_Parry",
	description = "Automatically parries ball",
	section = "left",
	callback = function(value)
		if getgenv().AutoParryNotify then
			if value then
				Library.SendNotification({
					title = "Module Notification",
					text = "Auto Parry has been turned ON",
					duration = 3,
				})
			else
				Library.SendNotification({
					title = "Module Notification",
					text = "Auto Parry has been turned OFF",
					duration = 3,
				})
			end
		end
		
		if value then
			if not getgenv().firstParryFired and not Parry_Key then
				Library.SendNotification({
					title = "🔍 Remote Getter",
					text = "Capturing remotes... (first parry)",
					duration = 3,
				})
				
				task.wait(0.5)
				
				performFirstPress(getgenv().firstParryType)
				
				getgenv().firstParryFired = true
				
				local captureAttempts = 0
				repeat
					task.wait(0.1)
					captureAttempts = captureAttempts + 1
				until Parry_Key ~= nil or captureAttempts >= 100
				
				if Parry_Key then
					Library.SendNotification({
						title = "✅ Captured!",
						text = "Remotes ready (" .. Parry_Key .. ")",
						duration = 3,
					})
					print("✅ Parry_Key:", Parry_Key)
				else
					Library.SendNotification({
						title = "❌ Capture Failed",
						text = "Manual parry needed",
						duration = 4,
					})
				end
			end

			Connections_Manager["Auto Parry"] = RunService.PreSimulation:Connect(function()
				local One_Ball = Auto_Parry.Get_Ball()
				local Balls = Auto_Parry.Get_Balls()
				for _, Ball in pairs(Balls) do
					if not Ball then
						return
					end
					local Zoomies = Ball:FindFirstChild("zoomies")
					if not Zoomies then
						return
					end
					Ball:GetAttributeChangedSignal("target"):Once(function()
						Parried = false
					end)
					if Parried then
						return
					end
					local Ball_Target = Ball:GetAttribute("target")
					local One_Target = One_Ball:GetAttribute("target")
					local Velocity = Zoomies.VectorVelocity
					local Distance = (Player.Character.PrimaryPart.Position - Ball.Position).Magnitude
					local Ping = game:GetService("Stats").Network.ServerStatsItem["Data Ping"]:GetValue() / 10
					local Ping_Threshold = math.clamp(Ping / 10, 5, 17)
					local Speed = Velocity.Magnitude
					local cappedSpeedDiff = math.min(math.max(Speed - 9.5, 0), 650)
					local speed_divisor_base = 2.4 + cappedSpeedDiff * 0.002
					local effectiveMultiplier = Speed_Divisor_Multiplier
					if getgenv().RandomParryAccuracyEnabled then
						if Speed < 200 then
							effectiveMultiplier = 0.7 + (math.random(40, 100) - 1) * (0.35 / 99)
						else
							effectiveMultiplier = 0.7 + (math.random(1, 100) - 1) * (0.35 / 99)
						end
					end
					local speed_divisor = speed_divisor_base * effectiveMultiplier
					local Parry_Accuracy = Ping_Threshold + math.max(Speed / speed_divisor, 9.5)
					local Curved = Auto_Parry.Is_Curved()
					if Ball:FindFirstChild("AeroDynamicSlashVFX") then
						Debris:AddItem(Ball.AeroDynamicSlashVFX, 0)
						Tornado_Time = tick()
					end
					if Runtime:FindFirstChild("Tornado") then
						if (tick() - Tornado_Time) < (Runtime.Tornado:GetAttribute("TornadoTime") or 1) + 0.314159 then
							return
						end
					end
					if One_Target == tostring(Player) and Curved then
						return
					end
					if Ball:FindFirstChild("ComboCounter") then
						return
					end
					local Singularity_Cape = Player.Character.PrimaryPart:FindFirstChild("SingularityCape")
					if Singularity_Cape then
						return
					end
					if getgenv().InfinityDetection and Infinity then
						return
					end
					if getgenv().DeathSlashDetection and deathshit then
						return
					end
					if getgenv().TimeHoleDetection and timehole then
						return
					end
					if Ball_Target == tostring(Player) and Distance <= Parry_Accuracy then
						if getgenv().AutoAbility and AutoAbility() then
							return
						end
					end
					if Ball_Target == tostring(Player) and Distance <= Parry_Accuracy then
						if getgenv().CooldownProtection and cooldownProtection() then
							return
						end
						local Parry_Time = os.clock()
						local Time_View = Parry_Time - Last_Parry
						if Time_View > 0.12 then
							Auto_Parry.Parry_Animation()
						end
						if getgenv().AutoParryKeypress then
							VirtualInputService:SendKeyEvent(true, Enum.KeyCode.F, false, nil)
						else
							Auto_Parry.Parry(Selected_Parry_Type)
						end
						Last_Parry = Parry_Time
						Parried = true
					end
					local Last_Parrys = tick()
					repeat
						RunService.PreSimulation:Wait()
					until (tick() - Last_Parrys) >= 0.3 or not Parried
					Parried = false
				end
			end)
		else
			if Connections_Manager["Auto Parry"] then
				Connections_Manager["Auto Parry"]:Disconnect()
				Connections_Manager["Auto Parry"] = nil
			end
		end
	end,
})

local dropdown3 = module:create_dropdown({
	title = "First Parry Type",
	flag = "First_Parry_Type",
	options = {
		"F_Key",
		"Left_Click",
		"Navigation",
	},
	multi_dropdown = false,
	maximum_options = 3,
	callback = function(value)
		firstParryType = value
	end,
})

local parryTypeMap = {
	["Camera"] = "Camera",
	["Slowball"] = "Slowball",
	["Random"] = "Random",
	["Backwards"] = "Backwards",
	["Straight"] = "Straight",
	["High"] = "High",
	["Left"] = "Left",
	["Right"] = "Right",
	["Random Target"] = "RandomTarget",
}

local dropdown = module:create_dropdown({
	title = "Parry Type",
	flag = "Parry_Type",
	options = {
		"Camera",
		"Slowball",
		"Random",
		"Backwards",
		"Straight",
		"High",
		"Left",
		"Right",
		"Random Target",
	},
	multi_dropdown = false,
	maximum_options = 8,
	callback = function(value)
		Selected_Parry_Type = parryTypeMap[value] or value
	end,
})

local UserInputService = game:GetService("UserInputService")
local parryOptions = {
	[Enum.KeyCode.One] = "Camera",
	[Enum.KeyCode.Two] = "Random",
	[Enum.KeyCode.Three] = "Backwards",
	[Enum.KeyCode.Four] = "Straight",
	[Enum.KeyCode.Five] = "High",
	[Enum.KeyCode.Six] = "Left",
	[Enum.KeyCode.Seven] = "Right",
	[Enum.KeyCode.Eight] = "Random Target",
	[Enum.KeyCode.Nine] = "Slowball",
}

UserInputService.InputBegan:Connect(function(input, gameProcessed)
	if gameProcessed then
		return
	end
	if not getgenv().HotkeyParryType then
		return
	end
	local newType = parryOptions[input.KeyCode]
	if newType then
		Selected_Parry_Type = parryTypeMap[newType] or newType
		dropdown:update(newType)
		if getgenv().HotkeyParryTypeNotify then
			Library.SendNotification({
				title = "Module Notification",
				text = "Parry Type changed to " .. newType,
				duration = 3,
			})
		end
	end
end)

module:create_slider({
	title = "Parry Accuracy",
	flag = "Parry_Accuracy",
	maximum_value = 100,
	minimum_value = 1,
	value = 100,
	round_number = true,
	callback = function(value)
		Speed_Divisor_Multiplier = 0.7 + (value - 1) * (0.35 / 99)
	end,
})

module:create_divider({})

module:create_checkbox({
	title = "Randomized Parry Accuracy",
	flag = "Random_Parry_Accuracy",
	callback = function(value)
		getgenv().RandomParryAccuracyEnabled = value
		if value then
			getgenv().RandomParryAccuracyEnabled = value
		end
	end,
})

module:create_checkbox({
	title = "Anti Phantom",
	flag = "Anti_Phantom",
	callback = function(value)
		getgenv().PhantomV2Detection = value
	end,
})

module:create_checkbox({
	title = "Cooldown Protection",
	flag = "CooldownProtection",
	callback = function(value)
		getgenv().CooldownProtection = value
	end,
})

module:create_checkbox({
	title = "Notify",
	flag = "Auto_Parry_Notify",
	callback = function(value)
		getgenv().AutoParryNotify = value
	end,
})

	local Triggerbot = parry:create_module({
		title = "Triggerbot",
		flag = "Triggerbot",
		description = "Instantly hits ball when targeted",
		section = "left",
		callback = function(value: boolean)
			if getgenv().TriggerbotNotify then
				if value then
					Library.SendNotification({
						title = "Module Notification",
						text = "Triggerbot turned ON",
						duration = 3,
					})
				else
					Library.SendNotification({
						title = "Module Notification",
						text = "Triggerbot turned OFF",
						duration = 3,
					})
				end
			end
			if value then
				Connections_Manager["Triggerbot"] = RunService.PreSimulation:Connect(function()
					local Balls = Auto_Parry.Get_Balls()
					for _, Ball in pairs(Balls) do
						if not Ball then
							return
						end
						Ball:GetAttributeChangedSignal("target"):Once(function()
							TriggerbotParried = false
						end)
						if TriggerbotParried then
							return
						end
						local Ball_Target = Ball:GetAttribute("target")
						local Singularity_Cape = Player.Character.PrimaryPart:FindFirstChild("SingularityCape")
						if Singularity_Cape then
							return
						end
						if getgenv().TriggerbotInfinityDetection and Infinity then
							return
						end
						if Ball_Target == tostring(Player) then
							if getgenv().TriggerbotKeypress then
								VirtualInputManager:SendKeyEvent(true, Enum.KeyCode.F, false, game)
							else
								Auto_Parry.Parry(Selected_Parry_Type)
							end
							TriggerbotParried = true
						end
						local Triggerbot_Last_Parrys = tick()
						repeat
							RunService.PreSimulation:Wait()
						until (tick() - Triggerbot_Last_Parrys) >= 1 or not TriggerbotParried
						TriggerbotParried = false
					end
				end)
			else
				if Connections_Manager["Triggerbot"] then
					Connections_Manager["Triggerbot"]:Disconnect()
					Connections_Manager["Triggerbot"] = nil
				end
			end
		end,
	})
	Triggerbot:create_checkbox({
		title = "Infinity Detection",
		flag = "Infinity_Detection",
		callback = function(value: boolean)
			getgenv().TriggerbotInfinityDetection = value
        end
    })

    Triggerbot:create_checkbox({
		title = "Notify",
		flag = "TriggerbotNotify",
		callback = function(value: boolean)
			getgenv().TriggerbotNotify = value
		end,
	})
local SpamParry = spam:create_module({
	title = "Auto Spam Parry",
	flag = "Auto_Spam_Parry",
	description = "Automatically spam parries ball",
	section = "left",
	callback = function(value)
		if getgenv().AutoSpamNotify then
			if value then
				Library.SendNotification({
					title = "Module Notification",
					text = "Auto Spam Parry turned ON",
					duration = 3,
				})
			else
				Library.SendNotification({
					title = "Module Notification",
					text = "Auto Spam Parry turned OFF",
					duration = 3,
				})
			end
		end
		if value then
			Connections_Manager["Auto Spam"] = RunService.PreSimulation:Connect(function()
				local Ball = Auto_Parry.Get_Ball()
				if not Ball then
					return
				end
				local Zoomies = Ball:FindFirstChild("zoomies")
				if not Zoomies then
					return
				end
				Auto_Parry.Closest_Player()
				local Ping = game:GetService("Stats").Network.ServerStatsItem["Data Ping"]:GetValue()
				local Ping_Threshold = math.clamp(Ping / 10, 0.8, 20)
				local Ball_Target = Ball:GetAttribute("target")
				local Ball_Properties = Auto_Parry:Get_Ball_Properties()
				local Entity_Properties = Auto_Parry:Get_Entity_Properties()
				local Spam_Accuracy = Auto_Parry.Spam_Service({
					Ball_Properties = Ball_Properties,
					Entity_Properties = Entity_Properties,
					Ping = Ping_Threshold,
				})
				local Target_Position = Closest_Entity.PrimaryPart.Position
				local Target_Distance = Player:DistanceFromCharacter(Target_Position)
				local Direction = (Player.Character.PrimaryPart.Position - Ball.Position).Unit
				local Ball_Direction = Zoomies.VectorVelocity.Unit
				local Dot = Direction:Dot(Ball_Direction)
				local Distance = Player:DistanceFromCharacter(Ball.Position)
				if not Ball_Target then
                    return
				end
				if Target_Distance > Spam_Accuracy or Distance > Spam_Accuracy then
					return
				end
				local Pulsed = Player.Character:GetAttribute("Pulsed")
				if Pulsed then
					return
				end
				if Ball_Target == tostring(Player) and Target_Distance > 30 and Distance > 30 then
					return
				end
				local threshold = ParryThreshold
				if Distance <= Spam_Accuracy and Parries > threshold then
					if getgenv().SpamParryKeypress then
						if Distance > Spam_Accuracy or Target_Distance > Spam_Accuracy then
							return
						end
						VirtualInputManager:SendKeyEvent(true, Enum.KeyCode.F, false, game)
					else
						Auto_Parry.Parry(Selected_Parry_Type)
					end
				end
			end)
		else
			if Connections_Manager["Auto Spam"] then
				Connections_Manager["Auto Spam"]:Disconnect()
				Connections_Manager["Auto Spam"] = nil
			end
		end
	end,
})

local dropdown2 = SpamParry:create_dropdown({
	title = "Parry Type",
	flag = "Spam_Parry_Type",
	options = {
		"Legit",
		"Blatant",
	},
	multi_dropdown = false,
	maximum_options = 2,
	callback = function(value) end,
})

SpamParry:create_slider({
	title = "Parry Threshold",
	flag = "Parry_Threshold",
	maximum_value = 3,
	minimum_value = 1,
	value = 2.5,
	round_number = false,
	callback = function(value)
		ParryThreshold = value
	end,
})

SpamParry:create_divider({})

if not isMobile then
	local RunService = game:GetService("RunService")
	local VirtualInputManager = game:GetService("VirtualInputManager")
	local Players = game:GetService("Players")
	local Player = Players.LocalPlayer

	local ParryThreshold = 3
	local Last_Parry = 0

	local AnimationFix = SpamParry:create_checkbox({
		title = "Animation Fix",
		flag = "AnimationFix",
		callback = function(enabled)
			if Connections_Manager["Animation Fix"] then
				Connections_Manager["Animation Fix"]:Disconnect()
				Connections_Manager["Animation Fix"] = nil
			end

			if not enabled then return end

			Connections_Manager["Animation Fix"] = RunService.PreSimulation:Connect(function()
				local Ball = Auto_Parry.Get_Ball()
				if not Ball then return end

				local Zoomies = Ball:FindFirstChild("zoomies")
				if not Zoomies then return end

				local closest = Auto_Parry.Closest_Player()
				if not closest or not closest.Entity then return end
				local Closest_Entity = closest.Entity
				local Closest_Distance = closest.Distance

				local Ping = game:GetService("Stats").Network.ServerStatsItem["Data Ping"]:GetValue()
				local Ping_Threshold = math.clamp(Ping / 10, 0.8, 20)

				local Ball_Target = Ball:GetAttribute("target")
				if not Ball_Target then return end

				local Ball_Properties = Auto_Parry:Get_Ball_Properties()
				local Entity_Properties = Auto_Parry:Get_Entity_Properties()

				local Spam_Accuracy = Auto_Parry.Spam_Service({
					Ball_Properties = Ball_Properties,
					Entity_Properties = Entity_Properties,
					Ping = Ping_Threshold,
				})

				local MyChar = Player.Character
				if not MyChar or not MyChar.PrimaryPart then return end
				local MyPos = MyChar.PrimaryPart.Position

				local BallPos = Ball.Position
				local TargetPos = Closest_Entity.PrimaryPart.Position

				local DistToBall = (MyPos - BallPos).Magnitude
				local DistToTarget = (MyPos - TargetPos).Magnitude

				if DistToTarget > Spam_Accuracy or DistToBall > Spam_Accuracy then return end

				if MyChar:GetAttribute("Pulsed") then return end

				if Ball_Target == tostring(Player) and DistToTarget > 30 and DistToBall > 30 then return end

				local Parries = Player:GetAttribute("Parries") or 0
				if DistToBall <= Spam_Accuracy and Parries > ParryThreshold then
					if tick() - Last_Parry < 0.02 then return end

					Last_Parry = tick()
					VirtualInputManager:SendKeyEvent(true, Enum.KeyCode.F, false, game)
					task.spawn(function()
				task.wait(0)
			VirtualInputManager:SendKeyEvent(false, Enum.KeyCode.F, false, game)
					end)
				end
			end)
		end,
	})

	AnimationFix:change_state(true)
end

SpamParry:create_checkbox({
	title = "Keypress",
	flag = "Auto_Spam_Parry_Keypress",
	callback = function(value)
		getgenv().SpamParryKeypress = value
	end,
})

SpamParry:create_checkbox({
	title = "Notify",
	flag = "Auto_Spam_Parry_Notify",
	callback = function(value)
		getgenv().AutoSpamNotify = value
	end,
})

local ManualSpam = spam:create_module({
	title = "Manual Spam Parry",
	flag = "Manual_Spam_Parry",
	description = "Manually Spams Parry",
	section = "right",
	callback = function(value)
		if getgenv().ManualSpamNotify then
			if value then
				Library.SendNotification({
					title = "Module Notification",
					text = "Manual Spam Parry turned ON",
					duration = 3,
				})
			else
				Library.SendNotification({
					title = "Module Notification",
					text = "Manual Spam Parry turned OFF",
					duration = 3,
				})
			end
		end
		if value then
			Connections_Manager["Manual Spam"] = RunService.PreSimulation:Connect(function()
				if getgenv().spamui then
					return
				end
				if getgenv().ManualSpamKeypress then
					VirtualInputManager:SendKeyEvent(true, Enum.KeyCode.F, false, game)
				else
					Auto_Parry.Parry(Selected_Parry_Type)
				end
			end)
		else
			if Connections_Manager["Manual Spam"] then
				Connections_Manager["Manual Spam"]:Disconnect()
				Connections_Manager["Manual Spam"] = nil
			end
		end
	end,
})

ManualSpam:change_state(false)

if isMobile then
	ManualSpam:create_checkbox({
		title = "UI",
		flag = "Manual_Spam_UI",
		callback = function(value)
			getgenv().spamui = value
			if value then
				local gui = Instance.new("ScreenGui")
				gui.Name = "ManualSpamUI"
				gui.ResetOnSpawn = false
				gui.Parent = game.CoreGui
				local frame = Instance.new("Frame")
				frame.Name = "MainFrame"
				frame.Position = UDim2.new(0, 20, 0, 20)
				frame.Size = UDim2.new(0, 200, 0, 100)
				frame.BackgroundColor3 = Color3.fromRGB(90, 60, 180)
				frame.BackgroundTransparency = 0.25
				frame.BorderSizePixel = 0
				frame.Active = true
				frame.Draggable = true
				frame.Parent = gui
				local uiCorner = Instance.new("UICorner")
				uiCorner.CornerRadius = UDim.new(0, 12)
				uiCorner.Parent = frame
				local uiStroke = Instance.new("UIStroke")
				uiStroke.Thickness = 2
				uiStroke.Color = Color3.fromRGB(190, 150, 255)
				uiStroke.Transparency = 0.2
				uiStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
				uiStroke.Parent = frame
				local uiGradient = Instance.new("UIGradient")
				uiGradient.Color = ColorSequence.new({
					ColorSequenceKeypoint.new(0, Color3.fromRGB(90, 60, 180)),
					ColorSequenceKeypoint.new(1, Color3.fromRGB(15, 10, 25)),
				})
				uiGradient.Rotation = 0
				uiGradient.Parent = frame
				local button = Instance.new("TextButton")
				button.Name = "ClashModeButton"
				button.Text = "Clash Mode"
				button.Size = UDim2.new(0, 160, 0, 40)
				button.Position = UDim2.new(0.5, -80, 0.5, -20)
				button.BackgroundTransparency = 1
				button.BorderSizePixel = 0
				button.Font = Enum.Font.GothamSemibold
				button.TextColor3 = Color3.fromRGB(255,255,255)
				button.TextSize = 22
				button.Parent = frame
				local activated = false
				local function toggle()
					activated = not activated
					button.Text = activated and "Stop" or "Clash Mode"
					if activated then
						Connections_Manager["Manual Spam UI"] = game:GetService("RunService").Heartbeat:Connect(function()
							Auto_Parry.Parry(Selected_Parry_Type)
						end)
					else
						if Connections_Manager["Manual Spam UI"] then
							Connections_Manager["Manual Spam UI"]:Disconnect()
							Connections_Manager["Manual Spam UI"] = nil
						end
					end
				end
				button.MouseButton1Click:Connect(toggle)
			else
				if game.CoreGui:FindFirstChild("ManualSpamUI") then
					game.CoreGui:FindFirstChild("ManualSpamUI"):Destroy()
				end
				if Connections_Manager["Manual Spam UI"] then
					Connections_Manager["Manual Spam UI"]:Disconnect()
					Connections_Manager["Manual Spam UI"] = nil
				end
			end
		end,
	})
end

ManualSpam:create_checkbox({
	title = "Keypress",
	flag = "Manual_Spam_Keypress",
	callback = function(value)
		getgenv().ManualSpamKeypress = value
	end,
})

ManualSpam:create_checkbox({
	title = "Notify",
	flag = "Manual_Spam_Parry_Notify",
	callback = function(value)
		getgenv().ManualSpamNotify = value
	end,
})

ReplicatedStorage.Remotes.ParrySuccessAll.OnClientEvent:Connect(function(_, root)
	if root.Parent and root.Parent ~= Player.Character then
		if root.Parent.Parent ~= workspace.Alive then
			return
		end
	end
	Auto_Parry.Closest_Player()
	local Ball = Auto_Parry.Get_Ball()
	if not Ball then
		return
	end
	local Target_Distance = (Player.Character.PrimaryPart.Position - Closest_Entity.PrimaryPart.Position).Magnitude
	local Distance = (Player.Character.PrimaryPart.Position - Ball.Position).Magnitude
	local Direction = (Player.Character.PrimaryPart.Position - Ball.Position).Unit
	local Dot = Direction:Dot(Ball.AssemblyLinearVelocity.Unit)
	local Curve_Detected = Auto_Parry.Is_Curved()
	if Target_Distance < 15 and Distance < 15 and Dot > 0 then
		if Curve_Detected then
			Auto_Parry.Parry(Selected_Parry_Type)
		end
	end
	if not Grab_Parry then
		return
	end
	Grab_Parry:Stop()
end)

ReplicatedStorage.Remotes.ParrySuccess.OnClientEvent:Connect(function()
	if Player.Character.Parent ~= workspace.Alive then
		return
	end
	if not Grab_Parry then
		return
	end
	Grab_Parry:Stop()
end)

workspace.Balls.ChildAdded:Connect(function()
	Parried = false
end)

workspace.Balls.ChildRemoved:Connect(function(Value)
	Parries = 0
	Parried = false
	if Connections_Manager["Target Change"] then
		Connections_Manager["Target Change"]:Disconnect()
		Connections_Manager["Target Change"] = nil
	end
end)

main:load()

