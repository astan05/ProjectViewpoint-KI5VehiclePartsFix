-- Reuse KI5's existing interior roof; no original assets or vehicle items are changed.
VPVPartsPorscheRoof = VPVPartsPorscheRoof or {}
local patch = VPVPartsPorscheRoof
local scriptId = "Base.82porsche911turbo"
local modelId = "PRS82intRoof"

function patch.prepare()
    local manager = getScriptManager()
    local script = manager:getVehicle(scriptId)
    if not script then return false end
    local part = script:getPartById("GloveBox")
    if not part then return false end
    if part:getModelById(modelId) then return true end
    if not manager:getModelScript("Base.82porsche911InteriorRoof") then
        print("[VPVParts] Porsche roof skipped: KI5 roof model unavailable")
        return false
    end
    script:Load(script:getName(), [[{
        part GloveBox {
            model PRS82intRoof { file = 82porsche911InteriorRoof, }
        }
    }]])
    print("[VPVParts] Porsche Turbo interior roof attached")
    return part:getModelById(modelId) ~= nil
end

function patch.applyVehicle(vehicle)
    if not vehicle or not vehicle:getScript() or vehicle:getScript():getFullName() ~= scriptId then return end
    if not patch.prepare() then return end
    local part = vehicle:getPartById("GloveBox")
    if part then
        part:setModelVisible(modelId, true)
    end
end

function patch.scan()
    if not patch.prepare() then return end
    -- Only the occupied car needs an interior roof. Avoid IsoCell's vehicle Set.
    for i = 0, getNumActivePlayers() - 1 do
        local player = getSpecificPlayer(i)
        if player then patch.applyVehicle(player:getVehicle()) end
    end
end

function patch.enter(player)
    if player then patch.applyVehicle(player:getVehicle()) end
end

Events.OnGameBoot.Add(patch.prepare)
Events.OnGameStart.Add(patch.scan)
Events.OnEnterVehicle.Add(patch.enter)

