package cmd

import (
	"context"
	"piday/agones-mc-oci/internal/config"
	"piday/agones-mc-oci/pkg/backup/oracle"

	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

var loadCmd = cobra.Command{
	Use:   "load",
	Short: "Loads minecraft world from Cloud Storage",
	Long:  "Load is an init container process that will load a minecraft world save/backup from Cloud Storage and load it into a volume",
	Run: func(cmd *cobra.Command, args []string) {
		cfg := config.NewLoadConfig()

		if cfg.GetBackupName() == "" {
			logger.Info("no backup annotation. creating a new world")
			return
		}

		logger.Info("loading saved world", zap.String("serverName", cfg.GetPodName()), zap.String("backupName", cfg.GetBackupName()))

		if err := RunLoad(cfg); err != nil {
			logger.Fatal("world loading failed", zap.String("serverName", cfg.GetPodName()), zap.String("backupName", cfg.GetBackupName()))
		}
		logger.Info("world loading succeeded", zap.String("serverName", cfg.GetPodName()), zap.String("backupName", cfg.GetBackupName()))
	},
}

func init() {
	RootCmd.AddCommand(&loadCmd)
}

func RunLoad(cfg config.LoadConfig) error {
	client, err := oracle.New(context.Background(), cfg.GetBucketName())
	if err != nil {
		logger.Error("error connecting to bucket", zap.Error(err))
		return err
	}

	if err := client.Load(cfg.GetBackupName(), cfg.GetVolume()); err != nil {
		logger.Error("error loading world", zap.Error(err))
		return err
	}

	return nil
}
