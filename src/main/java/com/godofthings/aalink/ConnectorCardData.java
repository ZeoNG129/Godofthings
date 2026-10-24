package com.godofthings.aalink;

import java.util.Optional;
import java.util.UUID;

/**
 * 无线连接器卡的持久化数据（照抄上游 OverloadedFrequencyCardData 的结构）。
 * <p>
 * [本地改动] 上游绑定的目标是「高级无线过载控制器 + 频率 id」；本地没有频率系统，
 * 卡直接记录绑定的 {@link AaLinkConnectorBlockEntity 无线连接器}所在维度与坐标，
 * 语义与上游一致（一张卡锁定一个远端网络的入口）。
 */
public record ConnectorCardData(
        boolean autoConnect,
        Optional<String> boundConnectorDimension,
        Optional<Long> boundConnectorPos,
        Optional<UUID> ownerUuid
)
{
    public ConnectorCardData
    {
        boundConnectorDimension = boundConnectorDimension == null
                ? Optional.empty()
                : boundConnectorDimension;
        boundConnectorPos = boundConnectorPos == null ? Optional.empty() : boundConnectorPos;
        ownerUuid = ownerUuid == null ? Optional.empty() : ownerUuid;
    }

    public static ConnectorCardData empty()
    {
        return new ConnectorCardData(
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public boolean isBound()
    {
        return boundConnectorDimension.isPresent() && boundConnectorPos.isPresent();
    }

    public boolean canBeUsedBy(UUID playerUuid)
    {
        return ownerUuid.isPresent() && ownerUuid.get().equals(playerUuid);
    }

    public ConnectorCardData bind(String connectorDimension, long connectorPos, UUID owner)
    {
        if (connectorDimension == null || connectorDimension.isBlank())
        {
            return this;
        }
        return new ConnectorCardData(
                autoConnect,
                Optional.of(connectorDimension),
                Optional.of(connectorPos),
                Optional.ofNullable(owner));
    }

    public ConnectorCardData clearBinding()
    {
        return new ConnectorCardData(
                autoConnect,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public ConnectorCardData withAutoConnect(boolean enabled)
    {
        return new ConnectorCardData(
                enabled,
                boundConnectorDimension,
                boundConnectorPos,
                ownerUuid);
    }

    public ConnectorCardData toggleAutoConnect()
    {
        return withAutoConnect(!autoConnect);
    }
}
